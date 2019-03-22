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
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future

class MoneySourcesControllerSpec extends AmlsSpec
  with MockitoSugar
  with MustMatchers
  with PatienceConfiguration
  with IntegrationPatience
  with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    when(mockCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
      .thenReturn(Future.successful(None))

    when(mockCacheConnector.save[MoneyServiceBusiness](any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(CacheMap("TESTID", Map())))

    val controller = new MoneySourcesController(dataCacheConnector = mockCacheConnector,
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

  trait DealsInForeignCurrencyFixture extends Fixture {
    val newRequest = request.withFormUrlEncodedBody(
      "currencies[0]" -> "USD",
      "currencies[1]" -> "GBP",
      "currencies[2]" -> "BOB",
      "usesForeignCurrencies" -> "false",
      "bankMoneySource" -> "Yes",
      "bankNames" -> "Bank names",
      "wholesalerMoneySource" -> "Yes",
      "wholesalerNames" -> "wholesaler names",
      "customerMoneySource" -> "Yes"
    )
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
        val currentModel = MoneySources(
          None,
          None,
          Some(true))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        when(mockCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(MoneyServiceBusiness(whichCurrencies = Some(WhichCurrencies(Seq(), None, Some(currentModel)))))))

        val result = controller.get()(request)
        val document = Jsoup.parse(contentAsString(result))

        status(result) mustEqual OK

//        document.select("select[name=currencies[0]] > option[value=USD]").hasAttr("selected") must be(true)
        document.select("input[name=bankMoneySource][checked]").`val` mustEqual ""
        document.select("input[name=wholesalerMoneySource][checked]").`val` mustEqual ""
      }
    }

    "post is called " when {
      "data is valid and edit is false" should {
        "redirect to FXTransactions in the next 12 months controller" in new DealsInForeignCurrencyFixture with MoneyServiceBusinessTestData {

          mockCacheGetEntry[MoneyServiceBusiness](Some(completeMsb), MoneyServiceBusiness.key)


          val result = controller.post().apply(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.msb.routes.FXTransactionsInNext12MonthsController.get().url)
        }
      }
      "data is valid and edit is true" should {
        "redirect to Summary Controller" in new DealsInForeignCurrencyFixture with MoneyServiceBusinessTestData {
          val result = controller.post(edit = true).apply(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.msb.routes.SummaryController.get().url)
        }
      }
      "data is invalid" should {
        "return bad request" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            ("IncorrectData1", "IncorrectData2")
          )

          val result = controller.post().apply(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }
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
}