/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import controllers.responsiblepeople.address
import forms.DateOfChangeFormProvider
import models.responsiblepeople.TimeAtAddress.{ThreeYearsPlus, ZeroToFiveMonths}
import models.responsiblepeople._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import services.cache.Cache
import utils.AmlsSpec
import views.html.DateOfChangeView

import java.time.LocalDate
import scala.concurrent.Future

class CurrentAddressDateOfChangeControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val statusService = mock[StatusService]
    lazy val view     = inject[DateOfChangeView]
    val controller    = new CurrentAddressDateOfChangeController(
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = statusService,
      cc = mockMcc,
      formProvider = inject[DateOfChangeFormProvider],
      view = view
    )
  }

  val emptyCache = Cache.empty
  val cache      = mock[Cache]

  "CurrentAddressDateOfChangeController" must {
    "when get is called" must {
      "return view for Date of Change when given a valid request" in new Fixture {
        val responsiblePeople = ResponsiblePerson()

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = controller.get(0, false)(request)
        status(result) must be(OK)
      }
    }

    "when post is called" when {
      "RP is incomplete" must {
        "redirect to the how long at current address page" in new Fixture {
          val postRequest = FakeRequest(POST, routes.CurrentAddressDateOfChangeController.post(1).url)
            .withFormUrlEncodedBody(
              "dateOfChange.year"  -> "2010",
              "dateOfChange.month" -> "10",
              "dateOfChange.day"   -> "01"
            )

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA11AA")
          val currentAddress    = ResponsiblePersonCurrentAddress(UKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePerson(
            addressHistory = Some(history),
            personName = Some(PersonName("firstName", Some("middleName"), "LastName")),
            positions = Some(Positions(Set(BeneficialOwner), Some(PositionStartDate(LocalDate.of(2009, 1, 1)))))
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(controller.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, false)(postRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(address.routes.TimeAtCurrentAddressController.get(1, false).url))
        }
      }

      "RP is incomplete and date entered before RP start date" must {
        "redirect to the how long at current address page" in new Fixture {
          val postRequest = FakeRequest(POST, routes.CurrentAddressDateOfChangeController.post(1).url)
            .withFormUrlEncodedBody(
              "dateOfChange.year"  -> "2010",
              "dateOfChange.month" -> "10",
              "dateOfChange.day"   -> "01"
            )

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA11AA")
          val currentAddress    = ResponsiblePersonCurrentAddress(UKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePerson(
            addressHistory = Some(history),
            personName = Some(PersonName("firstName", Some("middleName"), "LastName")),
            positions = Some(Positions(Set(BeneficialOwner), Some(PositionStartDate(LocalDate.of(2011, 1, 1)))))
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(controller.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, false)(postRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(address.routes.TimeAtCurrentAddressController.get(1, false).url))
        }
      }

      "when RP is complete" must {
        "redirect to the detailed answers page" in new Fixture with ResponsiblePeopleValues {
          val postRequest = FakeRequest(POST, routes.CurrentAddressDateOfChangeController.post(1).url)
            .withFormUrlEncodedBody(
              "dateOfChange.year"  -> "2010",
              "dateOfChange.month" -> "10",
              "dateOfChange.day"   -> "01"
            )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(completeResponsiblePerson))))
          when(controller.dataCacheConnector.save[ResponsiblePerson](any(), meq(ResponsiblePerson.key), any())(any()))
            .thenReturn(Future.successful(cache))
          when(cache.getEntry[ResponsiblePerson](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(completeResponsiblePerson))

          val result = controller.post(1, false)(postRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url)
          )
        }
      }
    }
    "respond with BAD_REQUEST" when {
      "given invalid data" in new Fixture {

        val invalidPostRequest = FakeRequest(POST, routes.CurrentAddressDateOfChangeController.post(1).url)
          .withFormUrlEncodedBody("invalid" -> "data")

        val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA11AA")
        val currentAddress    = ResponsiblePersonCurrentAddress(UKAddress, Some(ThreeYearsPlus))
        val history           = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
        val responsiblePeople = ResponsiblePerson(
          addressHistory = Some(history),
          personName = Some(PersonName("firstName", Some("middleName"), "LastName")),
          positions = Some(Positions(Set(BeneficialOwner), Some(PositionStartDate(LocalDate.of(2009, 1, 1)))))
        )

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
        when(controller.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1, true)(invalidPostRequest)

        status(result) must be(BAD_REQUEST)

      }
    }
  }
}
