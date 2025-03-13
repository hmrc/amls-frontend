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
import forms.msb.SendLargestAmountsFormProvider
import models.Country
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.BusinessActivity.{MoneyServiceBusiness => MoneyServiceBusinessActivity}
import models.moneyservicebusiness.{MoneyServiceBusiness, MostTransactions, SendTheLargestAmountsOfMoney}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.msb.SendLargestAmountsOfMoneyView

class SendTheLargestAmountsOfMoneyControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with PatienceConfiguration
    with IntegrationPatience
    with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[SendLargestAmountsOfMoneyView]
    val controller = new SendTheLargestAmountsOfMoneyController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockCacheConnector,
      mockStatusService,
      mockServiceFlow,
      mockAutoComplete,
      cc = mockMcc,
      formProvider = inject[SendLargestAmountsFormProvider],
      view = view
    )

    mockCacheFetch[ServiceChangeRegister](None, None)
  }

  val emptyCache = Cache.empty

  "SendTheLargestAmountsOfMoneyController" must {
    "load the 'Where to Send The Largest Amounts Of Money' page" in new Fixture {
      mockIsNewActivityNewAuth(false)
      mockApplicationStatus(NotCompleted)
      mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.title() must be(
        messages("msb.send.the.largest.amounts.of.money.title") +
          " - " + messages("summary.msb") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "pre-populate the 'Where to Send The Largest Amounts Of Money' Page" in new Fixture {
      val msb = Some(
        MoneyServiceBusiness(
          sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB"))))
        )
      )

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
        status(result)          must be(OK)
        contentAsString(result) must include(messages("msb.send.the.largest.amounts.of.money.title"))
      }

      "application is in variation mode and no service has been added" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
        mockIsNewActivityNewAuth(false)

        val result = controller.get()(request)
        status(result)          must be(OK)
        contentAsString(result) must include(messages("msb.send.the.largest.amounts.of.money.title"))
      }
    }

    "on post with valid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.SendTheLargestAmountsOfMoneyController.post().url)
        .withFormUrlEncodedBody(
          "largestAmountsOfMoney[0]" -> "GS"
        )

      mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.MostTransactionsController.get().url))
    }

    "on post with valid data in edit mode when the next page's data is in the store" in new Fixture {

      val newRequest = FakeRequest(POST, routes.SendTheLargestAmountsOfMoneyController.post().url)
        .withFormUrlEncodedBody(
          "largestAmountsOfMoney[0]" -> "GB"
        )

      val incomingModel = MoneyServiceBusiness(
        mostTransactions = Some(
          MostTransactions(
            Seq(
              Country("United Kingdom", "UK")
            )
          )
        )
      )

      mockCacheFetch[MoneyServiceBusiness](Some(incomingModel), Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.MostTransactionsController.get(true).url))
    }

    "on post with valid data in edit mode when the next page's data isn't in the store" in new Fixture {

      val newRequest = FakeRequest(POST, routes.SendTheLargestAmountsOfMoneyController.post().url)
        .withFormUrlEncodedBody(
          "largestAmountsOfMoney[0]" -> "GB"
        )

      val incomingModel = MoneyServiceBusiness()

      mockCacheFetch[MoneyServiceBusiness](Some(incomingModel), Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.MostTransactionsController.get(true).url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.SendTheLargestAmountsOfMoneyController.post().url)
        .withFormUrlEncodedBody(
          "largestAmountsOfMoney[0]" -> ""
        )

      mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
      mockCacheSave[MoneyServiceBusiness]

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      contentAsString(result) must include(messages("error.invalid.countries.msb.sendlargestamount.country"))
    }
  }
}
