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

import connectors.DataCacheConnector
import models.Country
import models.businessmatching._
import models.moneyservicebusiness.{MoneyServiceBusiness, _}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import models.businessmatching.{MoneyServiceBusiness => MoneyServiceBusinessActivity}

import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new SummaryController(mockCacheConnector, mockStatusService, self.authConnector, mockServiceFlow)

    val completeModel = MoneyServiceBusiness(
      throughput = Some(ExpectedThroughput.Second),
      businessUseAnIPSP = Some(BusinessUseAnIPSPYes("name", "123456789123456")),
      identifyLinkedTransactions = Some(IdentifyLinkedTransactions(true)),
      Some(WhichCurrencies(
        Seq("USD", "GBP", "EUR"),
        usesForeignCurrencies = Some(false),
        Some(BankMoneySource("bank names")),
        Some(WholesalerMoneySource("Wholesaler Names")),
        Some(true))),
      sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
      fundsTransfer = Some(FundsTransfer(true)),
      branchesOrAgents = Some(BranchesOrAgents(Some(Seq(Country("United Kingdom", "GB"))))),
      sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
      mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963")),
      ceTransactionsInNext12Months = Some(CETransactionsInNext12Months("12345678963"))
    )

    when {
      mockServiceFlow.inNewServiceFlow(any())(any(), any(), any())
    } thenReturn Future.successful(false)

    when {
      mockStatusService.isPreSubmission(any(), any(), any())
    } thenReturn Future.successful(true)
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {

      val model = MoneyServiceBusiness(None)
      val msbServices = Some(
        MsbServices(
          Set(
            TransmittingMoney,
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )

      mockIsNewActivity(false)
      mockCacheFetchAll
      mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(msbServices = msbServices)), BusinessMatching.key)
      mockCacheGetEntry[MoneyServiceBusiness]((Some(model)), MoneyServiceBusiness.key)
      mockApplicationStatus(NotCompleted)

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("summary.checkyouranswers.title"))
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {
      when(controller.dataCache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      val msbServices = Some(
        MsbServices(
          Set(
            TransmittingMoney,
            CurrencyExchange,
            ChequeCashingNotScrapMetal,
            ChequeCashingScrapMetal
          )
        )
      )

      mockApplicationStatus(NotCompleted)
      mockCacheGetEntry[BusinessMatching]((Some(BusinessMatching(msbServices = msbServices))), BusinessMatching.key)
      mockCacheGetEntry[MoneyServiceBusiness](None, MoneyServiceBusiness.key)

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

    "hide edit link for involved in other, turnover expected from activities and amls turnover expected page" when {
      "application in variation mode" in new Fixture {

        val bm = Some(BusinessMatching(msbServices = Some(MsbServices(Set(TransmittingMoney,CurrencyExchange,
          ChequeCashingNotScrapMetal,
          ChequeCashingScrapMetal)))))

        mockIsNewActivity(false)
        mockCacheFetchAll
        mockCacheGetEntry[BusinessMatching](bm, BusinessMatching.key)
        mockCacheGetEntry[MoneyServiceBusiness](Some(completeModel), MoneyServiceBusiness.key)
        mockApplicationStatus(SubmissionDecisionApproved)

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementsByTag("section").get(0).getElementsByTag("a").hasClass("change-answer") must be(false)
        document.getElementsByTag("section").get(1).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(2).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(3).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(4).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(5).getElementsByTag("a").hasClass("change-answer") must be(false)
        document.getElementsByTag("section").get(6).getElementsByTag("a").hasClass("change-answer") must be(false)
        document.getElementsByTag("section").get(7).getElementsByTag("a").hasClass("change-answer") must be(false)
        document.getElementsByTag("section").get(8).getElementsByTag("a").hasClass("change-answer") must be(false)
        document.getElementsByTag("section").get(9).getElementsByTag("a").hasClass("change-answer") must be(false)
        document.getElementsByTag("section").get(10).getElementsByTag("a").hasClass("change-answer") must be(false)
      }
    }

    "show edit link" when {
      "application not in variation mode" in new Fixture {
        val bm = Some(BusinessMatching(msbServices = Some(MsbServices(Set(TransmittingMoney,CurrencyExchange,
          ChequeCashingNotScrapMetal,
          ChequeCashingScrapMetal)))))

        mockIsNewActivity(false)
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
        mockIsNewActivity(false)
        mockCacheFetch[MoneyServiceBusiness](Some(completeModel), Some(MoneyServiceBusiness.key))
        mockCacheSave[MoneyServiceBusiness]

        val result = controller.post()(request)

        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))

        verify(controller.dataCache).save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(completeModel.copy(hasAccepted = true)))(any(),any(),any())
      }
    }

    "redirect to NewServiceInformationController" when {
      "status is not pre-submission and activity has just been added" in new Fixture {
        mockCacheFetch[MoneyServiceBusiness](Some(completeModel))
        mockCacheSave[MoneyServiceBusiness]

        when {
          mockServiceFlow.inNewServiceFlow(any())(any(), any(), any())
        } thenReturn Future.successful(true)

        when {
          controller.statusService.isPreSubmission(any(), any(), any())
        } thenReturn Future.successful(false)

        val result = controller.post()(request)

        redirectLocation(result) mustBe Some(controllers.businessmatching.updateservice.routes.NewServiceInformationController.get().url)

      }
    }

  }
}
