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

package controllers.hvd

import connectors.DataCacheConnector
import models.hvd.Hvd
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import services.StatusService
import utils.AuthorisedFixture
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class ReceiveCashPaymentsControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ReceiveCashPaymentsController(mock[DataCacheConnector], mock[ServiceFlow], mock[StatusService], self.authConnector)

    when(controller.cacheConnector.fetch[Hvd](eqTo(Hvd.key))(any(), any(), any()))
      .thenReturn(Future.successful(None))

    when(controller.cacheConnector.save[Hvd](eqTo(Hvd.key), any())(any(), any(), any()))
      .thenReturn(Future.successful(new CacheMap("", Map.empty)))

    when(controller.serviceFlow.inNewServiceFlow(any())(any(), any(), any()))
      .thenReturn(Future.successful(false))
  }

  "ReceiveCashPaymentsController" must {

    "load the page" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      val result = controller.get()(request)
      status(result) mustEqual OK
    }

    "redirect to Page not found" when {
      "application is in variation mode" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(NOT_FOUND)
      }
    }
    "show a bad request with an invalid request" in new Fixture {

      val result = controller.post()(request)
      status(result) mustEqual BAD_REQUEST
    }

    "redirect to summary on successful edit" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "receivePayments" -> "false"
      )

      val result = controller.post(true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }

    "redirect to next page on successful submission" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "receivePayments" -> "false"
      )

      val result = controller.post(false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.PercentageOfCashPaymentOver15000Controller.get().url)
    }
  }
}
