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
import models.businessmatching.{MoneyServiceBusiness => MoneyServiceBusinessActivity, _}
import models.moneyservicebusiness.{MoneyServiceBusiness, _}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}

class MostTransactionsControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new MostTransactionsController(
      self.authConnector,
      mockCacheConnector,
      mockStatusService,
      mockServiceFlow
    )
  }

  "MostTransactionsController" must {

    "show an empty form on get with no data in store" in new Fixture {

      mockIsNewActivity(false)
      mockApplicationStatus(NotCompleted)
      mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 0
      document.select(".amls-error-summary").size mustEqual 0
    }

    "show a prefilled form when there is data in the store" in new Fixture {

      val model = MoneyServiceBusiness(
        mostTransactions = Some(
          MostTransactions(
            models.countries.take(3)
          )
        )
      )

      mockIsNewActivity(false)
      mockApplicationStatus(NotCompleted)
      mockCacheFetch[MoneyServiceBusiness](Some(model), Some(MoneyServiceBusiness.key))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 3
      document.select(".amls-error-summary").size mustEqual 0
    }

    "continue to show the correct view" when {
      "application is in variation mode but the service has just been added" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
        mockIsNewActivity(true, Some(MoneyServiceBusinessActivity))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("msb.most.transactions.title"))
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

    "return a Bad request with errors on invalid submission" in new Fixture {

      val result = controller.post()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual BAD_REQUEST

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 0
      document.select(".amls-error-summary").size mustEqual 1
    }

    "on valid submission (no edit) (CE)" in new Fixture {

      val msbServices = Some(
        MsbServices(
          Set(
            CurrencyExchange
          )
        )
      )
      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      mockCacheFetchAll
      mockCacheGetEntry[MoneyServiceBusiness](Some(incomingModel), MoneyServiceBusiness.key)
      mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
      mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key) )

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.CETransactionsInNext12MonthsController.get().url)
    }

    "on valid submission (no edit) (non-CE)" in new Fixture {

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )
      val msbServices = Some(
        MsbServices(
          Set(
            ChequeCashingScrapMetal
          )
        )
      )
      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      mockCacheFetchAll
      mockCacheGetEntry[MoneyServiceBusiness](Some(incomingModel), MoneyServiceBusiness.key)
      mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
      mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key) )

      val result = controller.post(edit = false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }

    "return a redirect to the summary page on valid submission where the next page data exists (edit) (CE)" in new Fixture {

      val msbServices = Some(
        MsbServices(
          Set(
            CurrencyExchange
          )
        )
      )

      val incomingModel = MoneyServiceBusiness(
        ceTransactionsInNext12Months = Some(CETransactionsInNext12Months(
          "1223131"
        ))
      )

      val outgoingModel = MoneyServiceBusiness(
        ceTransactionsInNext12Months = Some(CETransactionsInNext12Months(
          "1223131"
        )),
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )

      mockCacheFetchAll
      mockCacheGetEntry[MoneyServiceBusiness](Some(incomingModel), MoneyServiceBusiness.key)
      mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
      mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key) )

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }

    "return a redirect on valid submission where the next page data doesn't exist (edit) (CE)" in new Fixture {
      val msbServices = Some(
        MsbServices(
          Set(
            CurrencyExchange
          )
        )
      )
      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[0]" -> "GB"
      )

      mockCacheFetchAll
      mockCacheGetEntry[MoneyServiceBusiness](Some(incomingModel), MoneyServiceBusiness.key)
      mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
      mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key) )

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.CETransactionsInNext12MonthsController.get(true).url)
    }

    "return a redirect to the summary page on valid submission (edit) (non-CE)" in new Fixture {

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "mostTransactionsCountries[]" -> "GB"
      )
      val msbServices = Some(
        MsbServices(
          Set(
            ChequeCashingScrapMetal
          )
        )
      )
      mockCacheFetchAll
      mockCacheGetEntry[MoneyServiceBusiness](Some(incomingModel), MoneyServiceBusiness.key)
      mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
      mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key) )

      val result = controller.post(edit = true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }
  }

  "throw exception when Msb services in Business Matching returns none" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "mostTransactionsCountries[]" -> "GB"
    )

    val incomingModel = MoneyServiceBusiness()

    val outgoingModel = incomingModel.copy(
      sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
      hasChanged = true
    )

    mockCacheFetchAll
    mockCacheGetEntry[MoneyServiceBusiness](None, MoneyServiceBusiness.key)
    mockCacheGetEntry[MoneyServiceBusiness](Some(incomingModel), MoneyServiceBusiness.key)
    mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key) )


    a[Exception] must be thrownBy {
      ScalaFutures.whenReady(controller.post(true)(newRequest)) { x => x }
    }
  }
}
