/*
 * Copyright 2021 HM Revenue & Customs
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
import models.Country
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{MoneyServiceBusiness => MoneyServiceBusinessActivity}
import models.moneyservicebusiness.{MoneyServiceBusiness, MostTransactions, SendTheLargestAmountsOfMoney}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DependencyMocks}
import views.html.msb.send_largest_amounts_of_money

class SendTheLargestAmountsOfMoneyControllerSpec extends AmlsSpec with MockitoSugar with PatienceConfiguration with IntegrationPatience {

  trait Fixture extends DependencyMocks {
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[send_largest_amounts_of_money]
    val controller = new SendTheLargestAmountsOfMoneyController(
      SuccessfulAuthAction, ds = commonDependencies,
      mockCacheConnector,
      mockStatusService,
      mockServiceFlow,
      mockAutoComplete,
      cc = mockMcc,
      send_largest_amounts_of_money = view
    )

    mockCacheFetch[ServiceChangeRegister](None, None)
  }

  val emptyCache = CacheMap("", Map.empty)

  "SendTheLargestAmountsOfMoneyController" must {
    "load the 'Where to Send The Largest Amounts Of Money' page" in new Fixture  {
      mockIsNewActivityNewAuth(false)
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
        sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB"))))))

      mockIsNewActivityNewAuth(false)
      mockApplicationStatus(NotCompleted)
      mockCacheFetch[MoneyServiceBusiness](msb, Some(MoneyServiceBusiness.key))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("select[name=largestAmountsOfMoney[0]] > option[value=GB]").hasAttr("selected") must be(true)

    }

    "render the SendTheLargestAmountOfMoney view" when {
      "application is in variation mode and a service has just been added" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
        mockIsNewActivityNewAuth(true, Some(MoneyServiceBusinessActivity))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("msb.send.the.largest.amounts.of.money.title"))
      }

      "application is in variation mode and no service has been added" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
        mockIsNewActivityNewAuth(false)

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("msb.send.the.largest.amounts.of.money.title"))
      }
    }

    "on post with valid data" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "largestAmountsOfMoney[0]" -> "GS"
      )

      mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.MostTransactionsController.get().url))
    }

    "on post with valid data in edit mode when the next page's data is in the store" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "largestAmountsOfMoney[0]" -> "GB"
      )

      val incomingModel = MoneyServiceBusiness(
        mostTransactions = Some(MostTransactions(
          Seq(
            Country("United Kingdom", "UK")
          )
        ))
      )

      mockCacheFetch[MoneyServiceBusiness](Some(incomingModel), Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.MostTransactionsController.get(true).url))
    }

    "on post with valid data in edit mode when the next page's data isn't in the store" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "largestAmountsOfMoney[0]" -> "GB"
      )

      val incomingModel = MoneyServiceBusiness()

      mockCacheFetch[MoneyServiceBusiness](Some(incomingModel), Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.MostTransactionsController.get(true).url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "largestAmountsOfMoney[0]" -> ""
      )

      mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#largestAmountsOfMoney]").html() must include(Messages("error.invalid.countries.msb.sendlargestamount.country"))
    }
  }
}
