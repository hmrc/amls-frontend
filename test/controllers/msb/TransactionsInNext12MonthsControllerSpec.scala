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

package controllers.msb

import controllers.actions.SuccessfulAuthAction
import forms.msb.TransactionsInNext12MonthsFormProvider
import models.businessmatching.BusinessActivity.{MoneyServiceBusiness => MoneyServiceBusinessActivity}
import models.businessmatching.updateservice.ServiceChangeRegister
import models.moneyservicebusiness.{MoneyServiceBusiness, SendMoneyToOtherCountry, TransactionsInNext12Months}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.msb.TransactionsInNext12MonthsView

import scala.concurrent.Future

class TransactionsInNext12MonthsControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[TransactionsInNext12MonthsView]
    val controller = new TransactionsInNext12MonthsController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockCacheConnector,
      mockStatusService,
      mockServiceFlow,
      cc = mockMcc,
      formProvider = inject[TransactionsInNext12MonthsFormProvider],
      view = view
    )

    mockIsNewActivityNewAuth(false)
    mockCacheFetch[ServiceChangeRegister](None, None)
  }

  val emptyCache = Cache.empty

  "TransactionsInNext12MonthsController" must {

    "load the page 'How many transactions do you expect in the next 12 months?'" in new Fixture {

      mockApplicationStatus(NotCompleted)

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("msb.transactions.expected.title"))
    }

    "load the page 'How many transactions do you expect in the next 12 months?' with pre populated data" in new Fixture {

      mockApplicationStatus(NotCompleted)

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any())).thenReturn(
        Future.successful(
          Some(MoneyServiceBusiness(transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963"))))
        )
      )

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include("12345678963")

    }

    "load the page 'How many transactions do you expect in the next 12 months?'" when {
      "status is approved and the service has just been added" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)

        when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
          .thenReturn(Future.successful(None))

        mockIsNewActivityNewAuth(true, Some(MoneyServiceBusinessActivity))

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("msb.transactions.expected.title"))
      }
    }

    "Show error message when user has not filled the mandatory fields" in new Fixture {

      val newRequest = FakeRequest(POST, routes.TransactionsInNext12MonthsController.post().url)
        .withFormUrlEncodedBody(
          "txnAmount" -> ""
        )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)          must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.required.msb.transactions.in.12months"))

    }

    "on valid post" in new Fixture {

      val newRequest = FakeRequest(POST, routes.TransactionsInNext12MonthsController.post().url)
        .withFormUrlEncodedBody(
          "txnAmount" -> "12345678963"
        )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SendMoneyToOtherCountryController.get().url))
    }

    "on valid post in edit mode with the next page's data in the store" in new Fixture {

      val newRequest = FakeRequest(POST, routes.TransactionsInNext12MonthsController.post().url)
        .withFormUrlEncodedBody(
          "txnAmount" -> "12345678963"
        )

      val incomingModel = MoneyServiceBusiness(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true))
      )

      val outgoingModel = incomingModel.copy(
        transactionsInNext12Months = Some(
          TransactionsInNext12Months("12345678963")
        ),
        hasChanged = true
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Future.successful(Some(incomingModel)))

      when(
        controller.dataCacheConnector
          .save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any())
      ).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get.url))
    }

    "on valid post in edit mode without the next page's data in the store" in new Fixture {

      val newRequest = FakeRequest(POST, routes.TransactionsInNext12MonthsController.post().url)
        .withFormUrlEncodedBody(
          "txnAmount" -> "12345678963"
        )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        transactionsInNext12Months = Some(
          TransactionsInNext12Months("12345678963")
        ),
        hasChanged = true
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Future.successful(Some(incomingModel)))

      when(
        controller.dataCacheConnector
          .save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any())
      ).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SendMoneyToOtherCountryController.get(true).url))
    }
  }
}
