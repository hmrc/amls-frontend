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
import models.Country
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{MoneyServiceBusiness => MoneyServiceBusinessActivity, _}
import models.moneyservicebusiness.{MoneyServiceBusiness, _}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocksNewAuth}

import scala.concurrent.Future

class MostTransactionsControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocksNewAuth {
    self => val request = addToken(authRequest)

    val controller = new MostTransactionsController(
      SuccessfulAuthAction,
      mockCacheConnector,
      mockStatusService,
      mockServiceFlow,
      mockAutoComplete
    )

    mockCacheFetch[ServiceChangeRegister](None, None)
    mockCacheGetEntry[ServiceChangeRegister](Some(ServiceChangeRegister()), ServiceChangeRegister.key)
    mockApplicationStatusNewAuth(NotCompleted)

    when {
      mockStatusService.isPreSubmission(any(), any(), any())
    } thenReturn Future.successful(true)
  }

  "MostTransactionsController" must {

    "show an empty form on get with no data in store" in new Fixture {

      mockIsNewActivityNewAuth(false)
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

      mockIsNewActivityNewAuth(false)
      mockApplicationStatusNewAuth(NotCompleted)
      mockCacheFetch[MoneyServiceBusiness](Some(model), Some(MoneyServiceBusiness.key))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustEqual OK

      document.select("select").size mustEqual 3
      document.select("option[selected]").size mustEqual 3
      document.select(".amls-error-summary").size mustEqual 0
    }

    "render the SendTheLargestAmountOfMoney view" when {
      "application is in variation and a service has just been added" in new Fixture {
        mockApplicationStatusNewAuth(SubmissionDecisionApproved)
        mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
        mockIsNewActivityNewAuth(true, Some(MoneyServiceBusinessActivity))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("msb.most.transactions.title"))
      }

      "application is in variation mode and no service has been added" in new Fixture {
        mockApplicationStatusNewAuth(SubmissionDecisionApproved)
        mockCacheFetch[MoneyServiceBusiness](None, Some(MoneyServiceBusiness.key))
        mockIsNewActivityNewAuth(false)

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("msb.most.transactions.title"))
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

    "redirect to the CE 'Transactions' on submission" when {
      "edit is false and Currency Exchange is available" in new Fixture {
        val msbServices = Some(
          BusinessMatchingMsbServices(
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
        mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key))

        val result = controller.post()(newRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CETransactionsInNext12MonthsController.get().url)
      }

      "edit is false and we're adding MSB to an approved application (CE)" in new Fixture {
        val msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
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

        when {
          mockStatusService.isPreSubmission(any(), any(), any())
        } thenReturn Future.successful(false)

        mockCacheFetchAll
        mockCacheGetEntry[MoneyServiceBusiness](Some(incomingModel), MoneyServiceBusiness.key)
        mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
        mockCacheGetEntry[ServiceChangeRegister](Some(ServiceChangeRegister(Some(Set(MoneyServiceBusinessActivity)))), ServiceChangeRegister.key)
        mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key))

        val result = controller.post()(newRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CETransactionsInNext12MonthsController.get().url)
      }

      "edit is false and we're adding MSB to an approved application (CE, FX)" in new Fixture {
        val msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, ForeignExchange)))
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

        when {
          mockStatusService.isPreSubmission(any(), any(), any())
        } thenReturn Future.successful(false)

        mockCacheFetchAll
        mockCacheGetEntry[MoneyServiceBusiness](Some(incomingModel), MoneyServiceBusiness.key)
        mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
        mockCacheGetEntry[ServiceChangeRegister](Some(ServiceChangeRegister(Some(Set(MoneyServiceBusinessActivity)))), ServiceChangeRegister.key)
        mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key))

        val result = controller.post()(newRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CETransactionsInNext12MonthsController.get().url)
      }
    }

    "redirect to the FX 'Transactions' on submission" when {
      "edit is false and Foreign Exchange is available and Currency Exchange is not" in new Fixture {
        val msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))

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
        mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key))

        val result = controller.post()(newRequest)
        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.FXTransactionsInNext12MonthsController.get().url)
      }

      "edit is false and we're adding MSB to an approved application (CE)" in new Fixture {
        val msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))
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

        when {
          mockStatusService.isPreSubmission(any(), any(), any())
        } thenReturn Future.successful(false)

        mockCacheFetchAll
        mockCacheGetEntry[MoneyServiceBusiness](Some(incomingModel), MoneyServiceBusiness.key)
        mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
        mockCacheGetEntry[ServiceChangeRegister](Some(ServiceChangeRegister(Some(Set(MoneyServiceBusinessActivity)))), ServiceChangeRegister.key)
        mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key))

        val result = controller.post()(newRequest)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.FXTransactionsInNext12MonthsController.get().url)
      }
    }

    "redirect to CETransactionsInNext12Months on valid submission when CE is available but has not just been added to the application " in new Fixture {

      val msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
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
      mockCacheGetEntry[ServiceChangeRegister](Some(ServiceChangeRegister(addedSubSectors = Some(Set(TransmittingMoney)))), ServiceChangeRegister.key)
      mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key))
      
      when {
        mockStatusService.isPreSubmission(any(), any(), any())
      } thenReturn Future.successful(false)

      val result = controller.post()(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.CETransactionsInNext12MonthsController.get().url)
    }

    "on valid submission (no edit) (non-CE, non-FE)" in new Fixture {

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        mostTransactions = Some(
          MostTransactions(
            Seq(Country("United Kingdom", "GB"))
          )
        ), hasChanged = true
      )
      val msbServices = Some(
        BusinessMatchingMsbServices(
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

      val result = controller.post()(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
    }

    "return a redirect to the summary page on valid submission where the next page data exists (edit) (CE)" in new Fixture {

      val msbServices = Some(
        BusinessMatchingMsbServices(
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
      redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
    }

    trait NextPageDataDoesNotExistFixture extends Fixture {
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
      mockCacheSave[MoneyServiceBusiness](outgoingModel, Some(MoneyServiceBusiness.key) )
    }

    "return a redirect on valid submission where the next page data doesn't exist (edit) (CE)" in new NextPageDataDoesNotExistFixture {
      val msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
      mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)

      val result = controller.post(edit = true)(newRequest)
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.CETransactionsInNext12MonthsController.get(true).url)
    }

  "return a redirect on valid submission where the next page data doesn't exist (edit) (CE, FX)" in new NextPageDataDoesNotExistFixture {
      val msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, ForeignExchange)))
      mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)

      val result = controller.post(edit = true)(newRequest)
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.CETransactionsInNext12MonthsController.get(true).url)
  }

  "return a redirect on valid submission where the next page data doesn't exist (edit) (FX)" in new NextPageDataDoesNotExistFixture {
      val msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))
      mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)

      val result = controller.post(edit = true)(newRequest)
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.FXTransactionsInNext12MonthsController.get(true).url)
  }

    "return a redirect to the summary page on valid submission (edit) (non-CE, non-FX)" in new NextPageDataDoesNotExistFixture {
      val msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingScrapMetal)))
      mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)

      val result = controller.post(edit = true)(newRequest)
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
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
