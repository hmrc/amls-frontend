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

package controllers.msb

import models.businessmatching._
import models.moneyservicebusiness.{MoneyServiceBusiness, _}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future

class IdentifyLinkedTransactionsControllerSpec extends AmlsSpec with MockitoSugar  {

    trait Fixture extends AuthorisedFixture with DependencyMocks{
      self => val request = addToken(authRequest)
      val controller = new IdentifyLinkedTransactionsController(mockCacheConnector, authConnector = self.authConnector)
    }

  val cacheMap = mock[CacheMap]

  val completedMT = Some(BusinessUseAnIPSPNo)
  val completedCE = Some(CETransactionsInNext12Months("10"))
  val completedFX = Some(FXTransactionsInNext12Months("23"))

  val emptyCache = CacheMap("", Map.empty)

  "IdentifyLinkedTransactionsController" must {

    "load the page systems identify linked transactions" in new Fixture {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.linked.txn.title"))
    }

    "load the page systems identify linked transactions with pre populated data" in new Fixture  {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(MoneyServiceBusiness(
        identifyLinkedTransactions = Some(IdentifyLinkedTransactions(true))))))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.linked.txn.title"))

      document.select("input[name=linkedTxn][checked]").`val` mustEqual "true"

    }

    "Show error message when user has not filled the mandatory fields" in new Fixture  {

      val newRequest = request.withFormUrlEncodedBody(
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include (Messages("error.required.msb.linked.txn"))

    }

    "Navigate to next page if they have selected MT as a service" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "true"
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            TransmittingMoney,
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )
      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        identifyLinkedTransactions = Some(
          IdentifyLinkedTransactions(true)
        ), hasChanged = true
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.BusinessUseAnIPSPController.get().url))
    }

    "user selects Yes and nvigate to next page if they have selected CE as a service" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "true"
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        identifyLinkedTransactions = Some(
          IdentifyLinkedTransactions(true)
        ), hasChanged = true
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.CETransactionsInNext12MonthsController.get().url))
    }

    "user selects No and navigates to CE section if they have selected CE as a service" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "false"
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        identifyLinkedTransactions = Some(
          IdentifyLinkedTransactions(false)
        ), hasChanged = true
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.CETransactionsInNext12MonthsController.get().url))
    }

    "navigates to FX section if they have selected FX as a service" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "false"
      )
      val msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))
      when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(cacheMap)))
      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
              .thenReturn(Some(MoneyServiceBusiness()))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.FXTransactionsInNext12MonthsController.get().url))
    }

    "Navigate to next page if they have selected cheque cashing as a service" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "true"
      )

      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        identifyLinkedTransactions = Some(
          IdentifyLinkedTransactions(true)
        ), hasChanged = true
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
    }

    "Navigate to next page if they have selected cheque cashing as a servicein edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "true"
      )

      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        identifyLinkedTransactions = Some(
          IdentifyLinkedTransactions(true)
        ), hasChanged = true
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
    }

    "Navigate to Summary page in edit mode when all services are included and have data filled" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "true"
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            TransmittingMoney,
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )
      val incomingModel = MoneyServiceBusiness(
        businessUseAnIPSP = completedMT,
        ceTransactionsInNext12Months = completedCE
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
    }

    "Navigate to Summary page in edit mode when CE pages are included and have data filled" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "true"
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )
      val incomingModel = MoneyServiceBusiness(
        ceTransactionsInNext12Months = completedCE
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
    }

    "Navigate to Summary page in edit mode when FX pages are included and have data filled" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "true"
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            ForeignExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )
      val incomingModel = MoneyServiceBusiness(
        fxTransactionsInNext12Months = completedFX
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
              .thenReturn(Some(incomingModel))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
    }

    "Navigate to MT section in edit mode when MT data is not in the store" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "true"
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            TransmittingMoney,
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )
      val incomingModel = MoneyServiceBusiness(
        ceTransactionsInNext12Months = completedCE
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))
      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.BusinessUseAnIPSPController.get(true).url))
    }

    "Navigate to CE section in edit mode when CE data is not in the store" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "true"
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            TransmittingMoney,
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )
      val incomingModel = MoneyServiceBusiness(
        businessUseAnIPSP = completedMT
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))
      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.CETransactionsInNext12MonthsController.get(true).url))
    }

    "Navigate to FX section in edit mode when FX data is not in the store" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "true"
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            TransmittingMoney,
            ForeignExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )
      val incomingModel = MoneyServiceBusiness(
        businessUseAnIPSP = completedMT
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(cacheMap)))
      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
              .thenReturn(Some(incomingModel))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.FXTransactionsInNext12MonthsController.get(true).url))
    }

    "throw exception when msb services in Business Matching returns none" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody (
        "linkedTxn" -> "true"
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
        hasChanged = true
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(None)

      when(cacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))


      a[Exception] must be thrownBy {
        ScalaFutures.whenReady(controller.post(true)(newRequest)) { x => x }
      }
    }
  }
}
