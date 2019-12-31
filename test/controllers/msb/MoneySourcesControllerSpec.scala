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

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{MoneyServiceBusiness => MoneyServiceBusinessActivity, _}
import models.moneyservicebusiness.{MoneyServiceBusiness, _}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.http.cache.client.CacheMap
import utils._

import scala.concurrent.Future

class MoneySourcesControllerSpec extends AmlsSpec
  with MockitoSugar
  with MustMatchers
  with PatienceConfiguration
  with IntegrationPatience
  with ScalaFutures {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any(), any()))
      .thenReturn(Future.successful(None))

    when(mockCacheConnector.save[MoneyServiceBusiness](any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(CacheMap("TESTID", Map())))

    val controller = new MoneySourcesController(dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      statusService = mockStatusService,
      serviceFlow = mockServiceFlow, cc = mockMcc)

    mockIsNewActivityNewAuth(false)
    mockCacheFetch[ServiceChangeRegister](None, Some(ServiceChangeRegister.key))

    val cacheMap = mock[CacheMap]

    when(controller.dataCacheConnector.fetchAll(any())(any()))
      .thenReturn(Future.successful(Some(cacheMap)))
    val msbServices = Some(BusinessMatchingMsbServices(Set()))
    when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key))
      .thenReturn(Some(MoneyServiceBusiness()))
    when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
      .thenReturn(Some(BusinessMatching(msbServices = msbServices)))
  }

  trait DealsInForeignCurrencyFixture extends AuthorisedFixture with MoneyServiceBusinessTestData with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val newRequest = requestWithUrlEncodedBody(
      "bankMoneySource" -> "Yes",
      "bankNames" -> "Bank names",
      "wholesalerMoneySource" -> "Yes",
      "wholesalerNames" -> "wholesaler names",
      "customerMoneySource" -> "Yes")

    val cacheMap = mock[CacheMap]

    when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any(), any()))
      .thenReturn(Future.successful(Some(completeMsb.copy(whichCurrencies = Some(WhichCurrencies(Seq("USD"), Some(UsesForeignCurrenciesYes), Some(MoneySources(None, None, None))))))))

    when(mockCacheConnector.save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), any())(any(), any()))
      .thenReturn(Future.successful(cacheMap))

    val controller = new MoneySourcesController(dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      statusService = mock[StatusService],
      serviceFlow = mock[ServiceFlow],
      cc = mockMcc)

    val msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))

    when(mockCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any(), any()))
      .thenReturn(Future.successful(Some(BusinessMatching(msbServices = msbServices))))
  }


  "MoneySourcesController" when {
    "get is called" should {
      "succeed" when {
        "status is pre-submission" in new Fixture {
          mockApplicationStatus(NotCompleted)

          val resp = controller.get(false).apply(request)
          status(resp) must be(200)
        }

        "status is approved but the service has just been added" in new Fixture with ServiceFlowMocks {
          mockApplicationStatus(SubmissionDecisionApproved)

          mockIsNewActivityNewAuth(true, Some(MoneyServiceBusinessActivity))

          val resp = controller.get(false).apply(request)
          status(resp) must be(200)
        }
      }

      "show a pre-populated form when model contains data" in new Fixture {
        val currentModel = MoneySources(
          None,
          None,
          Some(true))

        mockApplicationStatus(NotCompleted)

        when(mockCacheConnector.fetch[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key))(any(), any()))
          .thenReturn(Future.successful(Some(MoneyServiceBusiness(whichCurrencies = Some(WhichCurrencies(Seq(), None, Some(currentModel)))))))

        val result = controller.get()(request)
        val document = Jsoup.parse(contentAsString(result))

        status(result) mustEqual OK

        document.select("select[name=currencies[0]] > option[value=USD]").hasAttr("selected")
        document.select("input[name=bankMoneySource][checked]").`val` mustEqual ""
        document.select("input[name=wholesalerMoneySource][checked]").`val` mustEqual ""
      }
    }

    "post is called " when {
      "data is valid and edit is false" should {
        "redirect to FXTransactions in the next 12 months controller" in new DealsInForeignCurrencyFixture {
          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.msb.routes.FXTransactionsInNext12MonthsController.get().url)
        }
      }

      "data is valid and edit is true" should {
        "redirect to Summary Controller" in new DealsInForeignCurrencyFixture with MoneyServiceBusinessTestData {
          val result = controller.post(edit = true)(newRequest)

          mockCacheFetchAll

          mockCacheGetEntry[MoneyServiceBusiness](Some(MoneyServiceBusiness()), MoneyServiceBusiness.key)
          mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))))), BusinessMatching.key)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.msb.routes.SummaryController.get().url)
        }
      }

      "data is invalid" should {
        "return bad request" in new Fixture {
          val newRequest = requestWithUrlEncodedBody(
            ("IncorrectData1", "IncorrectData2")
          )

          val result = controller.post().apply(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }
    }

  }
}