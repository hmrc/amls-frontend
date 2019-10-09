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
import models.businessmatching._
import models.businessmatching.updateservice.ServiceChangeRegister
import models.moneyservicebusiness.{MoneyServiceBusiness, _}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.{ExecutionContext, Future}

class SummaryControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends DependencyMocks {
    self => val request = addToken(authRequest)
    implicit val ec = app.injector.instanceOf[ExecutionContext]

    val controller = new SummaryController(SuccessfulAuthAction, ds = commonDependencies, mockCacheConnector, mockStatusService, mockServiceFlow, mockMcc)

    val completeModel = MoneyServiceBusiness(
      throughput = Some(ExpectedThroughput.Second),
      businessUseAnIPSP = Some(BusinessUseAnIPSPYes("name", "123456789123456")),
      identifyLinkedTransactions = Some(IdentifyLinkedTransactions(true)),
      whichCurrencies = Some(WhichCurrencies(Seq("USD", "GBP", "EUR"), Some(UsesForeignCurrenciesYes), Some(MoneySources(None, None, Some(true))))),
      sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
      fundsTransfer = Some(FundsTransfer(true)),
      branchesOrAgents = Some(BranchesOrAgents(BranchesOrAgentsHasCountries(true), Some(BranchesOrAgentsWhichCountries(Seq(Country("United Kingdom", "GB")))))),
      sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
      mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963")),
      ceTransactionsInNext12Months = Some(CETransactionsInNext12Months("12345678963")),
      fxTransactionsInNext12Months = Some(FXTransactionsInNext12Months("3242342442"))
    )

    when {
      mockStatusService.isPreSubmission(any(), any(), any())(any(), any())
    } thenReturn Future.successful(true)

    mockCacheFetch[ServiceChangeRegister](None, None)
    mockCacheGetEntry[ServiceChangeRegister](None, ServiceChangeRegister.key)
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {

      val model = MoneyServiceBusiness(None)
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            TransmittingMoney,
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal,
            ForeignExchange
          )
        )
      )

      mockIsNewActivityNewAuth(false)
      mockCacheFetchAll
      mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
      mockCacheGetEntry[MoneyServiceBusiness]((Some(model)), MoneyServiceBusiness.key)
      mockApplicationStatus(NotCompleted)

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("summary.checkyouranswers.title"))
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {
      when(controller.dataCache.fetchAll(any())(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      val msbServices = Some(
        BusinessMatchingMsbServices(
          Set(
            TransmittingMoney,
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal,
            ForeignExchange
          )
        )
      )

      mockApplicationStatus(NotCompleted)
      mockCacheGetEntry[BusinessMatching]((Some(BusinessMatching(msbServices = msbServices))), BusinessMatching.key)
      mockCacheGetEntry[MoneyServiceBusiness](None, MoneyServiceBusiness.key)

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

    "show all edit link for involved in other, turnover expected from activities and amls turnover expected page" when {
      "application in variation mode" in new Fixture {

        val bm = Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney,CurrencyExchange,
          ChequeCashingNotScrapMetal,
          ChequeCashingScrapMetal,
          ForeignExchange)))))

        mockIsNewActivityNewAuth(false)
        mockCacheFetchAll
        mockCacheGetEntry[BusinessMatching](bm, BusinessMatching.key)
        mockCacheGetEntry[MoneyServiceBusiness](Some(completeModel), MoneyServiceBusiness.key)
        mockApplicationStatus(SubmissionDecisionApproved)

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementsByTag("section").get(0).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(1).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(2).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(3).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(4).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(5).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(6).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(7).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(8).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(9).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(10).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(11).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(12).getElementsByTag("a").hasClass("change-answer") must be(true)
      }
    }

    "show edit link" when {
      "application not in variation mode" in new Fixture {
        val bm = Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney,CurrencyExchange,
          ChequeCashingNotScrapMetal,
          ChequeCashingScrapMetal)))))

        mockIsNewActivityNewAuth(false)
        mockCacheFetchAll
        mockCacheGetEntry[BusinessMatching](bm, BusinessMatching.key)
        mockCacheGetEntry[MoneyServiceBusiness](Some(completeModel), MoneyServiceBusiness.key)
        mockApplicationStatus(NotCompleted)

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        val elements = document.getElementsByTag("section").iterator
        while(elements.hasNext){
          elements.next().getElementsByTag("a").hasClass("change-answer") must be(true)
        }
      }
    }
  }

  "Post" must {
    "redirect to RegistrationProgressController" when {
      "model has been saved with hasAccepted set to true" in new Fixture {
        mockIsNewActivityNewAuth(false)
        mockCacheFetch[MoneyServiceBusiness](Some(completeModel), Some(MoneyServiceBusiness.key))
        mockCacheSave[MoneyServiceBusiness]

        val result = controller.post()(request)

        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))

        verify(controller.dataCache).save[MoneyServiceBusiness](any(), eqTo(MoneyServiceBusiness.key), eqTo(completeModel.copy(hasAccepted = true)))(any(),any())
      }
    }
  }
}
