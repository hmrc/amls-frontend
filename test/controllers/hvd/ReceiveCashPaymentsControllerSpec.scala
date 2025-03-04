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

package controllers.hvd

import controllers.actions.SuccessfulAuthAction
import forms.hvd.ReceiveCashPaymentsFormProvider
import models.businessmatching.BusinessActivity.HighValueDealing
import models.businessmatching.updateservice.ServiceChangeRegister
import models.hvd.{Hvd, PaymentMethods}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, DependencyMocks}
import views.html.hvd.ReceiveCashView

class ReceiveCashPaymentsControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks { self =>

    val request    = addToken(authRequest)
    lazy val view  = inject[ReceiveCashView]
    val controller = new ReceiveCashPaymentsController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockCacheConnector,
      mockServiceFlow,
      mockStatusService,
      cc = mockMcc,
      formProvider = inject[ReceiveCashPaymentsFormProvider],
      view = view
    )

    mockCacheFetch[Hvd](None, Some(Hvd.key))
    mockCacheFetch[ServiceChangeRegister](None, None)
    mockCacheSave[Hvd]
    mockIsNewActivityNewAuth(false)
  }

  "ReceiveCashPaymentsController" must {

    "load the view" when {
      "status is pre-submission" in new Fixture {

        mockApplicationStatus(NotCompleted)

        val result = controller.get()(request)
        status(result) mustBe OK
      }

      "status is approved but the service has just been added" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)
        mockIsNewActivityNewAuth(true, Some(HighValueDealing))

        val result = controller.get()(request)
        status(result) mustBe OK
      }
    }

    "respond with bad request with an invalid request" in new Fixture {

      val result = controller.post()(request)
      status(result) mustBe BAD_REQUEST
    }

    "redirect to summary on edit" in new Fixture {

      val newRequest = FakeRequest(POST, routes.ReceiveCashPaymentsController.post().url)
        .withFormUrlEncodedBody(
          "receivePayments" -> "false"
        )

      val result = controller.post(true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.get.url)
    }

    "redirect to PercentageOfCashPaymentOver15000Controller on form equals no" in new Fixture {

      val newRequest = FakeRequest(POST, routes.ReceiveCashPaymentsController.post().url)
        .withFormUrlEncodedBody(
          "receivePayments" -> "false"
        )

      val result = controller.post()(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.PercentageOfCashPaymentOver15000Controller.get().url)

    }

    "redirect to ExpectToReceiveCashPaymentsController on form equals yes" when {
      "edit is true and hvd cashPaymentMethods is not defined" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ReceiveCashPaymentsController.post().url)
          .withFormUrlEncodedBody(
            "receivePayments" -> "true"
          )

        val result = controller.post(true)(newRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ExpectToReceiveCashPaymentsController.get(true).url)

      }
      "edit is false" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ReceiveCashPaymentsController.post().url)
          .withFormUrlEncodedBody(
            "receivePayments" -> "true"
          )

        val result = controller.post()(newRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ExpectToReceiveCashPaymentsController.get().url)

      }
    }
  }

  it must {
    "remove data from paymentMethods" when {
      "request is edit from yes to no" in new Fixture {

        mockCacheFetch[Hvd](
          Some(
            Hvd(
              receiveCashPayments = Some(true),
              cashPaymentMethods = Some(PaymentMethods(true, true, Some("")))
            )
          ),
          Some(Hvd.key)
        )

        val newRequest = FakeRequest(POST, routes.ReceiveCashPaymentsController.post().url)
          .withFormUrlEncodedBody(
            "receivePayments" -> "false"
          )

        val result = controller.post(true)(newRequest)

        status(result) mustEqual SEE_OTHER
        verify(controller.cacheConnector).save[Hvd](
          any(),
          any(),
          eqTo(
            Hvd(
              receiveCashPayments = Some(false),
              hasChanged = true
            )
          )
        )(any())
      }
    }
  }
}
