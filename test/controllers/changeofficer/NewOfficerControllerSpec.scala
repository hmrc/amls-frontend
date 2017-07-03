/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.changeofficer

import connectors.DataCacheConnector
import generators.ResponsiblePersonGenerator
import models.changeofficer.{NewOfficer, SoleProprietor, RoleInBusiness, ChangeOfficer}
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import org.jsoup.Jsoup
import org.scalacheck.Gen
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._

import scala.concurrent.Future


class NewOfficerControllerSpec extends GenericTestHelper with ResponsiblePersonGenerator {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(self.authRequest)

    val cache = mock[DataCacheConnector]

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(cache))
      .build()

    lazy val controller = injector.instanceOf[NewOfficerController]


  }

  "The NewOfficerController" when {
    "get is called" must {
      "get the view and show all the responsible people" in new TestFixture {

        val responsiblePeople = Gen.listOf(responsiblePeopleGen).sample.get

        when {
          cache.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any())
        } thenReturn Future.successful(Some(responsiblePeople))

        when {
          cache.fetch[ChangeOfficer](eqTo(ChangeOfficer.key))(any(), any(), any())
        } thenReturn Future.successful(Some(ChangeOfficer(RoleInBusiness(Set(SoleProprietor)))))

        val result = controller.get()(request)

        status(result) mustBe OK

        verify(cache).fetch(eqTo(ResponsiblePeople.key))(any(), any(), any())

        val html = Jsoup.parse(contentAsString(result))

        responsiblePeople.foreach { person =>
          html.select(s"input[type=radio][value=${person.personName.get.fullNameWithoutSpace}]").size() mustBe 1
        }
      }

      "prepopulate the view with the selected person" in new TestFixture {

        val responsiblePeople = Gen.listOfN(3, responsiblePeopleGen).sample.get :+
          ResponsiblePeople(Some(PersonName("Test", None, "Person", None, None)))

        val model = ChangeOfficer(RoleInBusiness(Set(SoleProprietor)), Some(NewOfficer("TestPerson")))

        when {
          cache.fetch[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any(), any(), any())
        } thenReturn Future.successful(Some(responsiblePeople))

        when {
          cache.fetch[ChangeOfficer](eqTo(ChangeOfficer.key))(any(), any(), any())
        } thenReturn Future.successful(Some(model))

        val result = controller.get()(request)

        status(result) mustBe OK

        val html = Jsoup.parse(contentAsString(result))

        html.select("input[type=radio][value=TestPerson]").hasAttr("checked") mustBe true
      }

    }

    "post is called" must {
      "respond with SEE_OTHER and redirect to the FurtherUpdatesController" in new TestFixture {

        when {
          cache.fetch[ChangeOfficer](any())(any(),any(), any())
        } thenReturn Future.successful(Some(ChangeOfficer(RoleInBusiness(Set(SoleProprietor)), None)))

        when {
          cache.save[ChangeOfficer](any(),any())(any(),any(),any())
        } thenReturn Future.successful(mock[CacheMap])

        val result = controller.post()(request.withFormUrlEncodedBody("person" -> "testName"))
        status(result) mustBe(SEE_OTHER)

        redirectLocation(result) mustBe Some(controllers.changeofficer.routes.FurtherUpdatesController.get().url)

        verify(cache).save(
          eqTo(ChangeOfficer.key),
          eqTo(ChangeOfficer(
            RoleInBusiness(Set(models.changeofficer.SoleProprietor)),
            Some(NewOfficer("testName"))
          )))(any(),any(),any())

      }
    }
  }

}
