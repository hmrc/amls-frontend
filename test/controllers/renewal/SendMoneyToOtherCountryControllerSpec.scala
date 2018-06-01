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

package controllers.renewal

import connectors.DataCacheConnector
import models.Country
import models.businessmatching._
import models.renewal.{CustomersOutsideUK, Renewal, SendMoneyToOtherCountry}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, AmlsSpec}
import cats.implicits._

import scala.concurrent.Future

class SendMoneyToOtherCountryControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val cacheMap = mock[CacheMap]

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockStatusService = mock[StatusService]
    lazy val mockRenewalService = mock[RenewalService]


    val controller = new SendMoneyToOtherCountryController(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )

    when {
      mockRenewalService.getRenewal(any(), any(), any())
    } thenReturn Future.successful(Renewal().some)

    when {
      mockRenewalService.updateRenewal(any())(any(), any(), any())
    } thenReturn Future.successful(cacheMap)

    when {
      mockDataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cacheMap))

    when {
      cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any())
    } thenReturn(Some(Renewal(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)))))
  }

  val emptyCache = CacheMap("", Map.empty)

  "SendMoneyToOtherCountryController" must {

    "load the page 'Did you send money to other countries in the past 12 months?'" in new Fixture {

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("renewal.msb.send.money.title"))
    }

    "load the page 'Do you send money to other countries?' with pre populated data" in new Fixture {

      when {
        mockRenewalService.getRenewal(any(), any(), any())
      } thenReturn Future.successful(Renewal(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true))).some)

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) must be(OK)
      contentAsString(result) must include(Messages("renewal.msb.send.money.title"))
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
      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))


      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(msbServices = msbServices)))

      when(controller.dataCacheConnector.save[Renewal](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.msb.send.money"))

    }

    "throw exception when Msb services in Business Matching returns none" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "money" -> "false"
      )

      val incomingModel = Renewal()

      val outgoingModel = incomingModel.copy(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
        hasChanged = true
      )

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(None)

      a[Exception] must be thrownBy {
        ScalaFutures.whenReady(controller.post(true)(newRequest)) { x => x }
      }
    }
  }

  "posting valid data" must {
    "redirect to the SendTheLargestAmountOfMoneyController" when {
      "post yes" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "money" -> "true"
        )

        when(cacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(
            msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          )))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SendTheLargestAmountsOfMoneyController.get().url))
      }
    }

    "redirect to the CETransactionsInLast12MonthsController" when {
      "post no and has currency exchange" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "money" -> "false"
        )

        when(cacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(
            msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange))),
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          )))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.CETransactionsInLast12MonthsController.get().url))
      }
    }

    "redirect to the PercentageOfCashPaymentOver15000Controller" when {
      "post no and has HVD and ASP and NOT CE" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "money" -> "false"
        )

        when(cacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(
            msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
            activities = Some(BusinessActivities(Set(HighValueDealing, AccountancyServices)))
          )))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.PercentageOfCashPaymentOver15000Controller.get().url))
      }
    }

    "redirect to the CustomersOutsideTheUKController" when {
      "post no and has HVD and NOT ASP or CE" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "money" -> "false"
        )

        when(cacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(
            msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          )))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.CustomersOutsideUKController.get().url))
      }
    }

    "redirect to the summary" when {
      "in edit mode" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "money" -> "false"
        )

        when(cacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(
            msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          )))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))

      }
      "not CE and not HVD" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "money" -> "false"
        )

        when(cacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(
            msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
            activities = Some(BusinessActivities(Set(AccountancyServices)))
          )))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
      }
    }
  }
}