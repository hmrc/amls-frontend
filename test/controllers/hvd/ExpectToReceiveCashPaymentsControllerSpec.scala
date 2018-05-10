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

package controllers.hvd

import models.hvd.Hvd
import models.status.{SubmissionDecisionApproved, SubmissionReady}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.ServiceFlow
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}
import models.businessmatching.HighValueDealing
import scala.concurrent.Future

class ExpectToReceiveCashPaymentsControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)

    val controller = new ExpectToReceiveCashPaymentsController(
      self.authConnector,
      mockCacheConnector,
      mockStatusService,
      mockServiceFlow
    )

    mockCacheFetch[Hvd](None, Some(Hvd.key))
    mockCacheSave[Hvd]
    mockApplicationStatus(SubmissionReady)
    mockIsNewActivity(false)
  }

  "ExpectToReceiveCashPaymentsController" when {

    "get is called" must {
      "display the expect_to_receive view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)

        val content = contentAsString(result)

        Jsoup.parse(content).title() must include(Messages("hvd.expect.to.receive.title"))
      }

      "display the view when supervised, but in the new service flow" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)
        mockIsNewActivity(true, Some(HighValueDealing))

        val result = controller.get()(request)

        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title() must include(Messages("hvd.expect.to.receive.title"))
      }

      "return NOT_FOUND" when {
        "registration is supervised" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.get()(request)

          status(result) must be(NOT_FOUND)

        }
      }
    }

    "post is called" when {
      "request is valid" must {
        "redirect to PercentageOfCashPaymentOver15000Controller" when {
          "edit is false" in new Fixture {

            val result = controller.post()(request.withFormUrlEncodedBody("courier" -> "true"))

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PercentageOfCashPaymentOver15000Controller.get().url))

          }
        }
        "redirect to SummaryController" when {
          "edit is true" in new Fixture {

            val result = controller.post(true)(request.withFormUrlEncodedBody("courier" -> "true"))

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))

          }
        }
      }
      "request is invalid" must {
        "respond with BAD_REQUEST" in new Fixture {

          val result = controller.post()(request)

          status(result) must be(BAD_REQUEST)

        }
      }
    }

  }

}
