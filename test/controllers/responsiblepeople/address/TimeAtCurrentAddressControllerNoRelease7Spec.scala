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
import forms.responsiblepeople.address.TimeAtAddressFormProvider
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ZeroToFiveMonths}
import models.responsiblepeople._
import models.status.SubmissionDecisionApproved
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.test
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.responsiblepeople.address.TimeAtAddressView

import scala.concurrent.Future

class TimeAtCurrentAddressControllerNoRelease7Spec extends AmlsSpec with Injecting {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val recordId = 1

  trait Fixture {
    self => val request = addToken(authRequest)
    lazy val view = inject[TimeAtAddressView]
    val timeAtAddressController = new TimeAtCurrentAddressController (
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      statusService = mock[StatusService],
      cc = mockMcc,
      formProvider = inject[TimeAtAddressFormProvider],
      view = view,
      error = errorView
    )
  }

  val emptyCache = CacheMap("", Map.empty)

  "CurrentAddressController" must {
    "when the service status is Approved and the address is changed" when {
      "time at address is less than 1 year" must {
        "redirect to the AdditionalAddressController" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.TimeAtCurrentAddressController.post(1).url)
          .withFormUrlEncodedBody(
            "timeAtAddress" -> ZeroToFiveMonths.toString
          )
          val ukAddress = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA11AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history), lineId = Some(1))

          when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(timeAtAddressController.statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = timeAtAddressController.post(recordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(recordId, true).url))
        }
      }
      "time at address is more than 1 year" must {
        "redirect to the correct location" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.TimeAtCurrentAddressController.post(1).url)
          .withFormUrlEncodedBody(
            "timeAtAddress" -> OneToThreeYears.toString
          )
          val ukAddress = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA11AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(OneToThreeYears))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history), lineId = Some(1))


          when(timeAtAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(timeAtAddressController.statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = timeAtAddressController.post(recordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(recordId).url))
        }
      }
    }
  }
}
