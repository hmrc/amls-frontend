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

import models.Country
import models.businessmatching.{MoneyServiceBusiness => MoneyServiceBusinessActivity}
import models.moneyservicebusiness.{MoneyServiceBusiness, MostTransactions, SendTheLargestAmountsOfMoney}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo}
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration}
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}

class SendTheLargestAmountsOfMoneyControllerSpec extends AmlsSpec with MockitoSugar with PatienceConfiguration with IntegrationPatience {

  trait Fixture extends AuthorisedFixture with DependencyMocks{
    self => val request = addToken(authRequest)

    val controller = new SendTheLargestAmountsOfMoneyController(
      self.authConnector,
      mockCacheConnector,
      mockStatusService,
      mockServiceFlow
    )
  }

  val emptyCache = CacheMap("", Map.empty)

  "SendTheLargestAmountsOfMoneyController" must {
    "load the 'Where to Send The Largest Amounts Of Money' page" in new Fixture  {
      mockIsNewActivity(false)
      mockApplicationStatus(NotCompleted)
      mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.title() must be (Messages("msb.send.the.largest.amounts.of.money.title") +
        " - " + Messages("summary.msb") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "pre-populate the 'Where to Send The Largest Amounts Of Money' Page" in new Fixture  {
      val msb = Some(MoneyServiceBusiness(
        sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB")))))

      mockIsNewActivity(false)
      mockApplicationStatus(NotCompleted)
      mockCacheFetch[MoneyServiceBusiness](msb, Some(MoneyServiceBusiness.key))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("select[name=country_1] > option[value=GB]").hasAttr("selected") must be(true)

    }

    "continue to show the correct view" when {
      "application is in variation mode but the service has just been added" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
        mockIsNewActivity(true, Some(MoneyServiceBusinessActivity))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("msb.send.the.largest.amounts.of.money.title"))
      }
    }


    "redirect to Page not found" when {
      "application is in variation mode" in new Fixture {
        mockIsNewActivity(false)
        mockApplicationStatus(SubmissionDecisionApproved)

        val result = controller.get()(request)
        status(result) must be(NOT_FOUND)
      }
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "country_1" -> "GS"
      )

      mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.MostTransactionsController.get().url))
    }

    "on post with valid data in edit mode when the next page's data is in the store" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "country_1" -> "GB"
      )

      val incomingModel = MoneyServiceBusiness(
        mostTransactions = Some(MostTransactions(
          Seq(
            Country("United Kingdom", "UK")
          )
        ))
      )

      val outgoingModel = incomingModel.copy(
        sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "UK")))
      )

      mockCacheFetch[MoneyServiceBusiness](Some(incomingModel), Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with valid data in edit mode when the next page's data isn't in the store" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "country_1" -> "GB"
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "UK")))
      )

      mockCacheFetch[MoneyServiceBusiness](Some(incomingModel), Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.MostTransactionsController.get(true).url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "country_1" -> ""
      )

      mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#country_1]").html() must include(Messages("error.required.country.name"))
    }
  }
}
