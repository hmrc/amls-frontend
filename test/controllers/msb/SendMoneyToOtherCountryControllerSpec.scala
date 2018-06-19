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
import models.businessmatching.updateservice.ServiceChangeRegister
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

class SendMoneyToOtherCountryControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)
    val controller = new SendMoneyToOtherCountryController(mockCacheConnector, self.authConnector, mockStatusService)

    mockCacheGetEntry[ServiceChangeRegister](None, ServiceChangeRegister.key)

    when {
      mockStatusService.isPreSubmission(any(), any(), any())
    } thenReturn Future.successful(true)

    val emptyCache = CacheMap("", Map.empty)
  }

  "SendMoneyToOtherCountryController" must {

    "load the page 'Do you send money to other countries?'" in new Fixture {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.send.money.title"))
    }

    "load the page 'Do you send money to other countries?' with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(MoneyServiceBusiness(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true))))))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.send.money.title"))
      document.select("input[name=money][checked]").`val` mustEqual "true"
    }

    "Show error message when user has not filled the mandatory fields" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
      )

      val msbServices = Some(BusinessMatchingMsbServices(
        Set(
          TransmittingMoney,
          CurrencyExchange
        )
      ))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.msb.send.money"))

    }

    "on valid post where the value is true" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "money" -> "true"
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
        hasChanged = true
      )
      val msbServices = Some(BusinessMatchingMsbServices(
        Set(
          TransmittingMoney,
          CurrencyExchange
        )
      ))
      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))


      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SendTheLargestAmountsOfMoneyController.get().url))
    }

    "on valid post where the value is false (CE)" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "money" -> "false"
      )
      val msbServices = Some(BusinessMatchingMsbServices(
        Set(
          TransmittingMoney,
          CurrencyExchange
        )
      ))

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
        hasChanged = true
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))


      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(false)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.CETransactionsInNext12MonthsController.get().url))
    }

    "redirect to the CE transactions page" when {
      "the application is not registered as Currency Exchange, but it has just been added to the application" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("money" -> "false")
        val msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
        val incomingModel = MoneyServiceBusiness()

        val outgoingModel = incomingModel.copy(
          sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
          hasChanged = true
        )

        mockCacheGetEntry[MoneyServiceBusiness](Some(incomingModel), MoneyServiceBusiness.key)
        mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
        mockCacheSave[MoneyServiceBusiness]

        mockCacheGetEntry[ServiceChangeRegister](Some(
          ServiceChangeRegister(addedSubSectors = Some(Set(CurrencyExchange)))), ServiceChangeRegister.key)

        when {
          controller.statusService.isPreSubmission(any(), any(), any())
        } thenReturn Future.successful(false)

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.CETransactionsInNext12MonthsController.get().url)
      }
    }

    "redirect to the summary page" when {
      "the application is registered as Currency Exchange, but it has not just been added to the application" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("money" -> "false")

        val msbServices = Some(BusinessMatchingMsbServices(
          Set(TransmittingMoney,
            CurrencyExchange)))

        val incomingModel = MoneyServiceBusiness()

        val outgoingModel = incomingModel.copy(
          sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
          hasChanged = true
        )

        when {
          controller.statusService.isPreSubmission(any(), any(), any())
        } thenReturn Future.successful(false)

        mockCacheGetEntry[MoneyServiceBusiness](Some(incomingModel), MoneyServiceBusiness.key)
        mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
        mockCacheSave[MoneyServiceBusiness]

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
      }
    }

    "on valid post where the value is false (Non-CE)" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "money" -> "false"
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
        hasChanged = true
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            TransmittingMoney
          )
        )
      )
      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(false)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
    }

    "on valid post where the value is true in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "money" -> "true"
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
        hasChanged = true
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            TransmittingMoney
          )
        )
      )
      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SendTheLargestAmountsOfMoneyController.get(true).url))
    }

    "on valid post where the value is false in edit mode (CE)" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "money" -> "false"
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            CurrencyExchange
          )
        )
      )
      val incomingModel = MoneyServiceBusiness(
        hasChanged = true
      )

      val outgoingModel = incomingModel.copy(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false))
      )
      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.CETransactionsInNext12MonthsController.get(true).url))
    }

    "on valid post where the value is false in edit mode (Non-CE)" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "money" -> "false"
      )

      val incomingModel = MoneyServiceBusiness()
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            TransmittingMoney
          )
        )
      )
      val outgoingModel = incomingModel.copy(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
        hasChanged = true
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
    }

    "throw exception when Msb services in Business Matching returns none" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "money" -> "false"
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
        hasChanged = true
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(incomingModel))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))


      a[Exception] must be thrownBy {
        ScalaFutures.whenReady(controller.post(true)(newRequest)) { x => x }
      }
    }
  }
}