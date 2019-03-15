/*
 * Copyright 2019 HM Revenue & Customs
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

import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbServices, MoneyServiceBusiness => MoneyServiceBusinessActivity}
import models.moneyservicebusiness._
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future

class SupplyForeignCurrenciesControllerSpec extends AmlsSpec
                                    with MockitoSugar
                                    with MustMatchers
                                    with PatienceConfiguration
                                    with IntegrationPatience
                                    with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    //val cache: DataCacheConnector = mock[DataCacheConnector]

    when(mockCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
      .thenReturn(Future.successful(None))

    when(mockCacheConnector.save[MoneyServiceBusiness](any(), any())(any(),any(),any()))
      .thenReturn(Future.successful(CacheMap("TESTID", Map())))

    val controller = new SupplyForeignCurrenciesController ( dataCacheConnector = mockCacheConnector,
      authConnector = self.authConnector,
      statusService = mockStatusService,
      serviceFlow = mockServiceFlow)

    mockIsNewActivity(false)
    mockCacheFetch[ServiceChangeRegister](None, Some(ServiceChangeRegister.key))

    val cacheMap = mock[CacheMap]

    when(controller.dataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(cacheMap)))
    val msbServices = Some(BusinessMatchingMsbServices(Set()))
    when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key))
            .thenReturn(Some(MoneyServiceBusiness()))
    when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
  }


  "WhichCurrencyController" when {
    "get is called" should {
      "succeed" when {
        "status is pre-submission" in new Fixture {
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          val resp = controller.get(false).apply(request)
          status(resp) must be(200)
        }

        "status is approved but the service has just been added" in new Fixture {
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          mockIsNewActivity(true, Some(MoneyServiceBusinessActivity))

          val resp = controller.get(false).apply(request)
          status(resp) must be(200)
        }
      }

      "show a pre-populated form when model contains data" in new Fixture {

        val currentModel = WhichCurrencies(
          Seq("USD")
//          ,
//          usesForeignCurrencies = Some(true),
//          None,
//          None,
//          Some(true)
        )

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        when(mockCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(MoneyServiceBusiness(whichCurrencies = Some(currentModel)))))

        val result = controller.get()(request)
        val document = Jsoup.parse(contentAsString(result))

        status(result) mustEqual OK

//        document.select("select[name=currencies[0]] > option[value=USD]").hasAttr("selected") must be(true)
//        document.select("input[name=usesForeignCurrencies][checked]").`val` mustEqual "Yes"
        document.select("input[name=bankMoneySource][checked]").`val` mustEqual ""
        document.select("input[name=wholesalerMoneySource][checked]").`val` mustEqual ""
//        document.select("input[name=customerMoneySource][checked]").`val` mustEqual "Yes"
      }
    }

    "redirect to Page not found" when {
      "application is in variation mode" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(NOT_FOUND)
      }
    }

    "post is called " when {
      "data is valid" should {
        trait DealsInForeignCurrencyFixture extends Fixture {
          val newRequest = request.withFormUrlEncodedBody (
            "currencies[0]" -> "USD",
            "currencies[1]" -> "GBP",
            "currencies[2]" -> "BOB",
            "bankMoneySource" ->"Yes",
            "bankNames" ->"Bank names",
            "wholesalerMoneySource" -> "Yes",
            "wholesalerNames" -> "wholesaler names",
            "customerMoneySource" -> "Yes",
            "usesForeignCurrencies" -> "Yes"
          )
        }

        trait NoDealsInForeignCurrencyFixture extends Fixture {
          val newRequest = request.withFormUrlEncodedBody (
            "currencies[0]" -> "USD",
            "currencies[1]" -> "GBP",
            "currencies[2]" -> "BOB",
            "usesForeignCurrencies" -> "No"
          )
        }

//        "user deals in foreign currency, redirect to check your answers when FX not an msb subservice" in new DealsInForeignCurrencyFixture {
//          val result = controller.post().apply(newRequest)
//          status(result) must be (SEE_OTHER)
//          redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
//        }
//
//        "user does not in foreign currency, redirect to check your answers when FX not an msb subservice" in new NoDealsInForeignCurrencyFixture {
//          val result = controller.post().apply(newRequest)
//          status(result) must be (SEE_OTHER)
//          redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
//        }
//
//        "user deals in foreign currency, go to FX transactions when FX an msb subservice" in new DealsInForeignCurrencyFixture {
//          override val msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))
//          when(controller.dataCacheConnector.fetchAll(any(), any()))
//                  .thenReturn(Future.successful(Some(cacheMap)))
//          when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
//                  .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
//          val result = controller.post().apply(newRequest)
//          status(result) must be (SEE_OTHER)
//          redirectLocation(result) mustEqual Some(routes.FXTransactionsInNext12MonthsController.get().url)
//        }
//
//        "user does not in foreign currency, go to FX transactions when FX an msb subservice" in new NoDealsInForeignCurrencyFixture {
//          override val msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))
//          when(controller.dataCacheConnector.fetchAll(any(), any()))
//                  .thenReturn(Future.successful(Some(cacheMap)))
//          when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
//                  .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
//          val result = controller.post(true).apply(newRequest)
//          status(result) must be (SEE_OTHER)
//          redirectLocation(result) mustEqual Some(routes.FXTransactionsInNext12MonthsController.get(true).url)
//        }
//
//        "user deals in foreign currency, redirect to FX transactions when FX transaction question unanswered and in edit mode" in new DealsInForeignCurrencyFixture {
//          override val msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))
//          when(controller.dataCacheConnector.fetchAll(any(), any()))
//                  .thenReturn(Future.successful(Some(cacheMap)))
//          when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
//                  .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
//          val result = controller.post(true).apply(newRequest)
//          status(result) must be (SEE_OTHER)
//          redirectLocation(result) mustEqual Some(routes.FXTransactionsInNext12MonthsController.get(true).url)
//        }
//
//        "user does not in foreign currency, redirect to FX transactions when FX transaction question unanswered and in edit mode" in new NoDealsInForeignCurrencyFixture {
//          override val msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))
//          when(controller.dataCacheConnector.fetchAll(any(), any()))
//                  .thenReturn(Future.successful(Some(cacheMap)))
//          when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
//                  .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
//          val result = controller.post(true).apply(newRequest)
//          status(result) must be (SEE_OTHER)
//          redirectLocation(result) mustEqual Some(routes.FXTransactionsInNext12MonthsController.get(true).url)
//        }
//
//        "user deals in foreign currency, redirect to check your answers when FX transaction question answered and in edit mode" in new DealsInForeignCurrencyFixture {
//          override val msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))
//          when(controller.dataCacheConnector.fetchAll(any(), any()))
//                  .thenReturn(Future.successful(Some(cacheMap)))
//          when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
//                  .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
//          val currentModel = FXTransactionsInNext12Months("1")
//          when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key))
//                  .thenReturn(Some(MoneyServiceBusiness(fxTransactionsInNext12Months = Some(currentModel))))
//          val result = controller.post(true).apply(newRequest)
//          status(result) must be (SEE_OTHER)
//          redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
//        }
//
//        "user does not deal in foreign currency, redirect to check your answers when FX transaction question answered and in edit mode" in new NoDealsInForeignCurrencyFixture {
//          override val msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
//          when(controller.dataCacheConnector.fetchAll(any(), any()))
//                  .thenReturn(Future.successful(Some(cacheMap)))
//          when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
//                  .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
//          val currentModel = FXTransactionsInNext12Months("1")
//          when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key))
//                  .thenReturn(Some(MoneyServiceBusiness(fxTransactionsInNext12Months = Some(currentModel))))
//          val result = controller.post(true).apply(newRequest)
//          status(result) must be (SEE_OTHER)
//          redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
//        }
//      }


//      "data is valid, but not using foreign currencies" should {
//        "clear the foreign currency data" in new Fixture {
//
//          val newRequest = request.withFormUrlEncodedBody (
//            "currencies[0]" -> "USD",
//            "currencies[1]" -> "GBP",
//            "currencies[2]" -> "BOB",
//            "bankMoneySource" ->"Yes",
//            "bankNames" ->"Bank names",
//            "wholesalerMoneySource" -> "Yes",
//            "wholesalerNames" -> "wholesaler names",
//            "customerMoneySource" -> "Yes",
//            "usesForeignCurrencies" -> "No"
//          )
//
//          val currentModel = WhichCurrencies(Seq("USD"), usesForeignCurrencies = Some(true), Some(mock[BankMoneySource]), Some(mock[WholesalerMoneySource]), Some(true))
//
//          when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any())).
//            thenReturn(Future.successful(Some(MoneyServiceBusiness(whichCurrencies = Some(currentModel)))))
//
//          val expectedModel = WhichCurrencies(Seq("USD", "GBP", "BOB"), usesForeignCurrencies = Some(false), None, None, None)
//          val result = controller.post(false).apply(newRequest)
//
//          status(result) must be(SEE_OTHER)
//
//          val captor = ArgumentCaptor.forClass(classOf[MoneyServiceBusiness])
//
//          verify(controller.dataCacheConnector).save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), captor.capture())(any(), any(), any())
//
//          captor.getValue match {
//            case result: MoneyServiceBusiness => result.whichCurrencies must be(Some(expectedModel))
//          }
//        }
//      }
//
//      "data is invalid" should {
//        "return invalid form" in new Fixture {
//
//          val newRequest = request.withFormUrlEncodedBody(
//            "bankMoneySource" ->"Yes",
//            "bankNames" ->"Bank names",
//            "wholesalerMoneySource" -> "Yes",
//            "wholesalerNames" -> "wholesaler names",
//            "customerMoneySource" -> "Yes"
//          )
//
//          whenReady(controller.post(false).apply(newRequest)) {
//            resp =>
//              resp.header.status must be(BAD_REQUEST)
//          }
//        }
      }
    }
  }
}
