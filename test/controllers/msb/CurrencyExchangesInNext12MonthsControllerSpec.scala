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
import forms.msb.CurrencyExchangesInNext12MonthsFormProvider
import models.businessmatching.BusinessActivity.{MoneyServiceBusiness => MoneyServiceBusinessActivity}
import models.businessmatching.updateservice.ServiceChangeRegister
import models.moneyservicebusiness._
import models.status.NotCompleted
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.msb.CurrencyExchangesInNext12MonthsView

import scala.concurrent.Future

class CurrencyExchangesInNext12MonthsControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[CurrencyExchangesInNext12MonthsView]
    val controller = new CurrencyExchangesInNext12MonthsController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      statusService = mockStatusService,
      serviceFlow = mockServiceFlow,
      cc = mockMcc,
      formProvider = inject[CurrencyExchangesInNext12MonthsFormProvider],
      view = view
    )

    mockIsNewActivityNewAuth(false)
    mockCacheFetch[ServiceChangeRegister](None, None)
  }

  val emptyCache = Cache.empty

  val fullModel = WhichCurrencies(
    Seq("USD", "CHF", "EUR"),
    Some(UsesForeignCurrenciesYes),
    Some(MoneySources(Some(BankMoneySource("Bank names")), Some(WholesalerMoneySource("wholesaler names")), Some(true)))
  )

  "CurrencyExchangesInNext12MonthsController" must {

    "load the page 'How many currency exchange transactions do you expect in the next 12 months?'" in new Fixture {

      mockApplicationStatus(NotCompleted)

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(messages("msb.ce.transactions.expected.in.12.months.title"))
    }

    "load the page 'How many currency exchange transactions do you expect in the next 12 months?' with pre populated data" in new Fixture {

      mockApplicationStatus(NotCompleted)

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any())).thenReturn(
        Future.successful(
          Some(MoneyServiceBusiness(ceTransactionsInNext12Months = Some(CETransactionsInNext12Months("12345678963"))))
        )
      )

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include("12345678963")
    }

    "load the page when the application status is approved and the service has just been added" in new Fixture {
      mockApplicationStatus(NotCompleted)

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      mockIsNewActivityNewAuth(true, Some(MoneyServiceBusinessActivity))

      val result = controller.get()(request)

      status(result)          must be(OK)
      contentAsString(result) must include(messages("msb.ce.transactions.expected.in.12.months.title"))
    }

    "Show error message when user has not filled the mandatory fields" in new Fixture {

      val newRequest = FakeRequest(POST, routes.CurrencyExchangesInNext12MonthsController.post().url)
        .withFormUrlEncodedBody(
          "ceTransaction" -> ""
        )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)          must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.required.msb.ce.transactions.in.12months"))

    }

    "Show error message when user has entered data in the wrong format" in new Fixture {

      val newRequest = FakeRequest(POST, routes.CurrencyExchangesInNext12MonthsController.post().url)
        .withFormUrlEncodedBody(
          "ceTransaction" -> "asas"
        )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)          must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.invalid.msb.ce.transactions.in.12months.number"))

    }

    "Successfully save data in mongoCache and navigate to Next page" in new Fixture {
      val newRequest = FakeRequest(POST, routes.CurrencyExchangesInNext12MonthsController.post().url)
        .withFormUrlEncodedBody(
          "ceTransaction" -> "12345678963"
        )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.WhichCurrenciesController.get().url))
    }

    "Successfully save data in mongoCache and navigate to Summary page in edit mode if the next page's data is in store" in new Fixture {

      val incomingModel = MoneyServiceBusiness(
        whichCurrencies = Some(fullModel)
      )

      val outgoingModel = incomingModel.copy(
        ceTransactionsInNext12Months = Some(
          CETransactionsInNext12Months("12345678963")
        ),
        hasChanged = true
      )

      val newRequest = FakeRequest(POST, routes.CurrencyExchangesInNext12MonthsController.post().url)
        .withFormUrlEncodedBody(
          "ceTransaction" -> "12345678963"
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

    "on valid submission (edit) without next page's data" in new Fixture {

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        ceTransactionsInNext12Months = Some(
          CETransactionsInNext12Months("12345678963")
        ),
        hasChanged = true
      )

      val newRequest = FakeRequest(POST, routes.CurrencyExchangesInNext12MonthsController.post().url)
        .withFormUrlEncodedBody(
          "ceTransaction" -> "12345678963"
        )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Future.successful(Some(incomingModel)))

      when(
        controller.dataCacheConnector
          .save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))(any())
      ).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.WhichCurrenciesController.get(true).url))
    }
  }
}
