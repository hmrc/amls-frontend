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
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.{SoleProprietor => BmSoleProprietor, _}
import models.changeofficer.{ChangeOfficer, Role, RoleInBusiness}
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class RoleInBusinessControllerSpec extends GenericTestHelper {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(self.authRequest)

    val cache = mock[DataCacheConnector]

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(cache))
      .build()

    lazy val controller = injector.instanceOf[RoleInBusinessController]

    val nominatedOfficer = ResponsiblePeople(
      personName = Some(PersonName("firstName", None, "lastName",None, None)),
      positions = Some(Positions(Set(NominatedOfficer),None))
    )

    val otherResponsiblePerson = ResponsiblePeople(
      personName = Some(PersonName("otherFirstName", None, "otherLastName",None, None)),
      positions = Some(Positions(Set(Director),None))
    )

    val details = ReviewDetails(
      "Some business",
      Some(BmSoleProprietor),
      Address("Line 1", "Line 2", None, None, None, Country("UK", "UK")),
      "XA123456789",
      None)

    when(cache.fetch[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any(), any(), any()))
      .thenReturn(Future.successful(Some(Seq(nominatedOfficer, otherResponsiblePerson))))

    when {
      cache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
    } thenReturn Future.successful(Some(BusinessMatching(Some(details))))

    when {
      cache.fetch[ChangeOfficer](eqTo(ChangeOfficer.key))(any(), any(), any())
    } thenReturn Future.successful(Some(ChangeOfficer(RoleInBusiness(Set(models.changeofficer.SoleProprietor)))))
  }

  "The RoleInBusinessController" must {
    "get the view" in new TestFixture {
      val result = controller.get()(request)

      status(result) mustBe OK
      contentAsString(result) must include("firstName lastName")

      contentAsString(result) must include(Messages("responsiblepeople.position_within_business.lbl.06"))
    }

    "populate the view" in new TestFixture {

      val result = controller.get()(request)

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      html.select("input[type=checkbox][value=06]").hasAttr("checked") mustBe true
    }

    "when post is called" must {
      "redirect to NewOfficerController" when {
        "a role is selected without 'none of the above' being selected" in new TestFixture {

          when(cache.save(any(), any())(any(),any(), any()))
            .thenReturn(Future.successful(mock[CacheMap]))

          val result = controller.post()(request.withFormUrlEncodedBody("positions[]" -> "06"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.NewOfficerController.get().url)

          verify(cache).save(eqTo(ChangeOfficer.key), eqTo(ChangeOfficer(RoleInBusiness(Set(models.changeofficer.SoleProprietor)))))(any(),any(),any())
        }
      }
      "redirect to RemoveResponsiblePersonController" when {
        "'none of the above' is selected" in new TestFixture {

          when(cache.save(any(), any())(any(),any(), any()))
            .thenReturn(Future.successful(mock[CacheMap]))

          val result = controller.post()(request.withFormUrlEncodedBody("positions[]" -> ""))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.RemoveResponsiblePersonController.get().url)

          verify(cache).save(eqTo(ChangeOfficer.key), eqTo(ChangeOfficer(RoleInBusiness(Set.empty[Role]))))(any(),any(),any())
        }
      }

      "respond with BAD_REQUEST when no options selected and show the error message and the name" in new TestFixture {
        val result = controller.post()(request)
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(Messages("changeofficer.roleinbusiness.validationerror"))
        contentAsString(result) must include(Messages("firstName lastName"))
      }
    }
  }

}
