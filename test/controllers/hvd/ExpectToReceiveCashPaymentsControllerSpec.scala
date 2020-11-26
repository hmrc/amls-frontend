/*
 * Copyright 2020 HM Revenue & Customs
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

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.HighValueDealing
import models.businessmatching.updateservice.ServiceChangeRegister
import models.hvd.Hvd
import models.status.{SubmissionDecisionApproved, SubmissionReady}
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks}
import views.html.hvd.expect_to_receive

class ExpectToReceiveCashPaymentsControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    lazy val view = app.injector.instanceOf[expect_to_receive]
    val controller =
      new ExpectToReceiveCashPaymentsController(
      SuccessfulAuthAction,
      ds = commonDependencies,
        mockCacheConnector,
        mockStatusService,
        mockServiceFlow,
        cc = mockMcc,
        expect_to_receive = view)

    mockCacheFetch[Hvd](None, Some(Hvd.key))
    mockCacheSave[Hvd]
    mockApplicationStatus(SubmissionReady)
    mockIsNewActivityNewAuth(false)
    mockCacheFetch[ServiceChangeRegister](None, Some(ServiceChangeRegister.key))
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
        mockIsNewActivityNewAuth(true, Some(HighValueDealing))

        val result = controller.get()(request)

        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title() must include(Messages("hvd.expect.to.receive.title"))
      }
    }

    "post is called" when {
      "request is valid" must {
        "redirect to PercentageOfCashPaymentOver15000Controller" when {
          "edit is false" in new Fixture {

            val result = controller.post()(requestWithUrlEncodedBody("courier" -> "true"))

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PercentageOfCashPaymentOver15000Controller.get().url))

          }
        }
        "redirect to SummaryController" when {
          "edit is true" in new Fixture {

            val result = controller.post(true)(requestWithUrlEncodedBody("courier" -> "true"))

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
        "check that error message -no option selected- exists in the request" in new Fixture {
          val message = Messages("error.required.hvd.choose.option")

          val result = controller.post(true)(requestWithUrlEncodedBody("" -> ""))

          val content = contentAsString(result)

          status(result) must be(BAD_REQUEST)
          Jsoup.parse(content).body().getElementsByClass("validation-summary-message").first().html() must include(message)
          Jsoup.parse(content).body().getElementById("paymentMethods").html() must include(message)
        }
      }
    }

  }

}
