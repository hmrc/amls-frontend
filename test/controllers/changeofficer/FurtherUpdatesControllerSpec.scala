/*
 * Copyright 2018 HM Revenue & Customs
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
import models.changeofficer.{ChangeOfficer, NewOfficer, RoleInBusiness, Director => Director$}
import models.responsiblepeople._
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, AmlsSpec}

import scala.concurrent.Future

class FurtherUpdatesControllerSpec extends AmlsSpec with MockitoSugar with PrivateMethodTester {

  trait TestFixture extends AuthorisedFixture {
    self =>
    val request = addToken(self.authRequest)

    val dataCacheConnector = mock[DataCacheConnector]
    val cacheMap = mock[CacheMap]

    val changeOfficer = ChangeOfficer(
      RoleInBusiness(Set.empty),
      Some(NewOfficer("NewOfficer"))
    )

    val newOfficer = ResponsiblePerson(
      personName = Some(PersonName("New", None, "Officer")),
      positions = Some(Positions(Set(
        DesignatedMember
      ), None)))

    val oldOfficer = ResponsiblePerson(
      personName = Some(PersonName("Old", None, "Officer")),
      positions = Some(Positions(Set(
        NominatedOfficer
      ), None)))

    val responsiblePeople = Seq(
      newOfficer,
      oldOfficer
    )

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .build()

    when {
      controller.dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cacheMap))

    when {
      controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any())(any(), any(), any())
    } thenReturn Future.successful(Some(responsiblePeople))

    when {
      cacheMap.getEntry[ChangeOfficer](meq(ChangeOfficer.key))(any())
    } thenReturn Some(changeOfficer)

    when {
      cacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())
    } thenReturn Some(responsiblePeople)

    when {
      controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any())(any(), any(), any())
    } thenReturn Future.successful(cacheMap)

    lazy val controller = injector.instanceOf[FurtherUpdatesController]
  }

  "The FurtherUpdatesController" when {

    "get is called" must {
      "display further_updates" in new TestFixture {
        val result = controller.get()(request)

        status(result) mustBe OK
        contentAsString(result) must include(Messages("changeofficer.furtherupdates.title"))
      }
    }

    "post is called" must {
      "redirect to WhoIsRegisteringController" when {
        "furtherUpdates equals no" in new TestFixture {
          val result = controller.post()(request.withFormUrlEncodedBody("furtherUpdates" -> "false"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.declaration.routes.WhoIsRegisteringController.get().url)
        }
      }
      "redirect to RegistrationProgressController" when {
        "furtherUpdates equals yes" in new TestFixture {
          val result = controller.post()(request.withFormUrlEncodedBody("furtherUpdates" -> "true"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)
        }
      }
      "return BAD_REQUEST" when {
        "form is invalid" in new TestFixture {
          val result = controller.post()(request)

          status(result) mustBe BAD_REQUEST
        }
      }
    }

    "addNominatedOfficer is called" must {
      "add NominatedOfficer to the positions of the given responsible person" in new TestFixture {

        val addNominatedOfficer = PrivateMethod[ResponsiblePerson]('addNominatedOfficer)

        val result = controller invokePrivate addNominatedOfficer(newOfficer)

        result must equal(newOfficer.copy(
          positions = Some(Positions(Set(DesignatedMember, NominatedOfficer), newOfficer.positions.get.startDate)),
          hasChanged = true
        ))

      }
    }

    "removeNominatedOfficers is called" must {
      "remove NominatedOfficer from the positions of the given responsible person" in new TestFixture {

        val removeNominatedOfficers = PrivateMethod[Seq[ResponsiblePerson]]('removeNominatedOfficers)

        val result = controller invokePrivate removeNominatedOfficers(responsiblePeople)

        result must equal(Seq(
          newOfficer,
          oldOfficer.copy(
            positions = Some(Positions(Set.empty[PositionWithinBusiness], oldOfficer.positions.get.startDate)),
            hasChanged = true
          )))
      }
    }

    "updateNominatedOfficers is called" must {
      "return a collection of responsible people with updated nominated officers" in new TestFixture {

        val updateNominatedOfficers = PrivateMethod[Seq[ResponsiblePerson]]('updateNominatedOfficers)

        val result = controller invokePrivate updateNominatedOfficers((oldOfficer, 1), RoleInBusiness(Set()), responsiblePeople, 0)

        result must equal(Seq(
          newOfficer.copy(
            positions = Some(Positions(newOfficer.positions.get.positions + NominatedOfficer, newOfficer.positions.get.startDate)),
            hasChanged = true,
            hasAccepted = true
          ),
          oldOfficer.copy(
            positions = Some(Positions(oldOfficer.positions.get.positions - NominatedOfficer, oldOfficer.positions.get.startDate)),
            hasChanged = true,
            hasAccepted = true
          )
        ))
      }
    }

  }

  it must {
    "update the roles of old officer and new before redirecting" in new TestFixture {

      override val changeOfficer = ChangeOfficer(
        RoleInBusiness(Set(Director$)),
        Some(NewOfficer("NewOfficer"))
      )

      when {
        cacheMap.getEntry[ChangeOfficer](meq(ChangeOfficer.key))(any())
      } thenReturn Some(changeOfficer)

      val result = controller.post()(request.withFormUrlEncodedBody("furtherUpdates" -> "false"))

      status(result) mustBe SEE_OTHER

      verify(controller.dataCacheConnector).save[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key), meq(Seq(
        newOfficer.copy(
          positions = Some(Positions(newOfficer.positions.get.positions + NominatedOfficer, newOfficer.positions.get.startDate)),
          hasChanged = true,
          hasAccepted = true
        ),
        oldOfficer.copy(
          positions = Some(Positions(
            oldOfficer.positions.get.positions - NominatedOfficer + Director,
            oldOfficer.positions.get.startDate)
          ),
          hasChanged = true,
          hasAccepted = true
        )
      )))(any(), any(), any())

    }
  }

}