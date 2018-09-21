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

package services

import connectors.DataCacheConnector
import models.Country
import models.businessmatching._
import models.moneyservicebusiness.{MoneyServiceBusiness => moneyServiceBusiness}
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.renewal._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class RenewalServiceSpec extends AmlsSpec with MockitoSugar {

  implicit val hc = HeaderCarrier()

  trait Fixture extends AuthorisedFixture {

    val dataCache = mock[DataCacheConnector]
    implicit val authContext = mock[AuthContext]

    val service = new RenewalService(dataCache)

    val mockCacheMap = mock[CacheMap]

    when(dataCache.fetchAll(any(),any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))
    when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
            .thenReturn(Some(BusinessMatching()))

    def setupBusinessMatching(activities: Set[BusinessActivity] = Set(), msbServices: Set[BusinessMatchingMsbService] = Set()) = when {
        mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(msbServices)), activities = Some(BusinessActivities(activities))))

    def setUpRenewal(renewalModel: Renewal) = when {
      dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
    } thenReturn Future.successful(Some(renewalModel))

    def standardCompleteInvolvedInOtherActivities(): Renewal = {
      Renewal(
        involvedInOtherActivities = Some(InvolvedInOtherYes("test")),
        turnover = Some(AMLSTurnover.First),
        businessTurnover = Some(BusinessTurnover.First),
        hasAccepted = true,
        hasChanged = true
      )
    }
  }

  "The renewal service" must {

    "return the correct section" when {

      "the renewal hasn't been started" in new Fixture {
        when {
          dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(None)

        val section = await(service.getSection)
        section mustBe Section(Renewal.sectionKey, NotStarted, hasChanged = false, controllers.renewal.routes.WhatYouNeedController.get())

      }

      "the renewal is complete and has been started" in new Fixture {
        setupBusinessMatching(Set(MoneyServiceBusiness, HighValueDealing), Set(CurrencyExchange, TransmittingMoney))

        val completeModel = Renewal(
          Some(InvolvedInOtherYes("test")),
          Some(BusinessTurnover.First),
          Some(AMLSTurnover.First),
          Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          Some(PercentageOfCashPaymentOver15000.First),
          Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
          Some(TotalThroughput("01")),
          Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
          Some(TransactionsInLast12Months("1500")),
          Some(SendTheLargestAmountsOfMoney(Country("us", "US"))),
          Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
          Some(CETransactionsInLast12Months("123")),
          Some(FXTransactionsInLast12Months("456")),
          true,
          Some(SendMoneyToOtherCountry(true))
        )

        setUpRenewal(completeModel)

        val section = await(service.getSection)
        await(service.isRenewalComplete(completeModel)) mustBe true
        section mustBe Section(Renewal.sectionKey, Completed, hasChanged = true, controllers.renewal.routes.SummaryController.get())
      }

      "the renewal model is not complete" in new Fixture {
        val renewal = mock[Renewal]
        when(renewal.hasChanged) thenReturn true

        setUpRenewal(renewal)

        val section = await(service.getSection)
        section mustBe Section(Renewal.sectionKey, Started, hasChanged = true, controllers.renewal.routes.WhatYouNeedController.get())
      }

      "the renewal model is not complete and not started" in new Fixture {
        val renewal = Renewal(None)
        setUpRenewal(renewal)

        val section = await(service.getSection)
        section mustBe Section(Renewal.sectionKey, NotStarted, hasChanged = false, controllers.renewal.routes.WhatYouNeedController.get())
      }
    }
  }

  trait StandardFixture extends Fixture {
    setupBusinessMatching(Set(TelephonePaymentService))
    val preFilledModel: Renewal = Renewal(hasChanged = true)
  }

  trait ASPFixture extends Fixture {
    setupBusinessMatching(Set(AccountancyServices))
    val preFilledModel = standardCompleteInvolvedInOtherActivities()
  }

  trait HVDFixture extends Fixture {
    setupBusinessMatching(Set(HighValueDealing))
    val preFilledModel = standardCompleteInvolvedInOtherActivities()
  }

  trait ASPHVDFixture extends Fixture {
    setupBusinessMatching(Set(AccountancyServices, HighValueDealing))
    val preFilledModel = standardCompleteInvolvedInOtherActivities()
  }

  trait MSBFixture extends Fixture {
    setupBusinessMatching(Set(MoneyServiceBusiness), Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal))
    val preFilledModel = standardCompleteInvolvedInOtherActivities()
  }

  trait MTFixture extends Fixture {
    setupBusinessMatching(Set(MoneyServiceBusiness), Set(TransmittingMoney))
    val preFilledModel = standardCompleteInvolvedInOtherActivities()
  }

  trait CEFixture extends Fixture {
    setupBusinessMatching(Set(MoneyServiceBusiness), Set(CurrencyExchange))
    val preFilledModel = standardCompleteInvolvedInOtherActivities()
  }

  trait FXFixture extends Fixture {
    setupBusinessMatching(Set(MoneyServiceBusiness), Set(ForeignExchange))
    val preFilledModel = standardCompleteInvolvedInOtherActivities()
  }

  trait AllFixture extends Fixture {
    setupBusinessMatching(Set(MoneyServiceBusiness), Set(TransmittingMoney, CurrencyExchange, ForeignExchange))
    val preFilledModel = standardCompleteInvolvedInOtherActivities()
  }

  "isRenewalComplete" must {
    "be true" when {

      "Standard renewal flow questions section is complete" when {
        "involvedInOtherActivities is true" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = Some(InvolvedInOtherYes("test")),
            turnover = Some(AMLSTurnover.First),
            businessTurnover = Some(BusinessTurnover.First),
            hasAccepted = true
          )
          await(service.isRenewalComplete(model)) mustBe true
        }

        "involvedInOtherActivites is false" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = Some(InvolvedInOtherNo),
            turnover = Some(AMLSTurnover.First),
            businessTurnover = None,
            hasAccepted = true
          )
          await(service.isRenewalComplete(model)) mustBe true
        }
      }

      "ASP is selected business activity and section is complete along with standard renewal flow questions" in new ASPFixture {
        val model = preFilledModel.copy(
          customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB")))))
        )
        await(service.isRenewalComplete(model)) mustBe true
      }

      "HVD is selected business activity and section is complete along with standard renewal flow questions" in new HVDFixture {
        val model = preFilledModel.copy(
          customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
          receiveCashPayments = Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other")))))
        )
        await(service.isRenewalComplete(model)) mustBe true
      }

      "ASP and HVD are selected business activities and section is complete along with standard renewal flow questions" in new ASPHVDFixture {
        val model = preFilledModel.copy(
          customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
          receiveCashPayments = Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other")))))
        )
        await(service.isRenewalComplete(model)) mustBe true
      }

      "MSB is selected business activity w/o MT, CE, FX subsectors and section is complete along with standard renewal flow questions" in new MSBFixture {
        val model = preFilledModel.copy(
          totalThroughput = Some(TotalThroughput("01"))
        )
        await(service.isRenewalComplete(model)) mustBe true
      }

      "MSB is selected business activity with MT subsector and w/o CE, FX subsectors and section is complete along with standard renewal flow questions" when {
        "sendMoneyToOtherCountries is true" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("us", "US"))),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB"))))
          )
          await(service.isRenewalComplete(model)) mustBe true
        }

        "sendMoneyToOtherCountries is false" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None
          )
          await(service.isRenewalComplete(model)) mustBe true
        }

        "sendMoneyToOtherCountries is None" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None
          )
          await(service.isRenewalComplete(model)) mustBe true
        }
      }

      "MSB is selected business activity with CE subsector and w/o MT, FX subsectors and section is complete along with standard renewal flow questions" in new CEFixture {
        val model = preFilledModel.copy(
          totalThroughput = Some(TotalThroughput("01")),
          whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
          ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123"))
        )
        await(service.isRenewalComplete(model)) mustBe true
      }

      "MSB is selected business activity with FX subsector and w/o MT, CE subsectors and section is complete along with standard renewal flow questions" in new FXFixture {
        val model = preFilledModel.copy(
          totalThroughput = Some(TotalThroughput("01")),
          fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("456"))
        )
        await(service.isRenewalComplete(model)) mustBe true
      }

      "MSB is selected business activity with MT, CE, FX subsectors and section is complete along with standard renewal flow questions" when {
        "sendMoneyToOtherCountries is true" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("us", "US"))),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123")),
            fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("456"))
          )
          await(service.isRenewalComplete(model)) mustBe true
        }

        "sendMoneyToOtherCountries is False" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None,
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123")),
            fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("456"))
          )
          await(service.isRenewalComplete(model)) mustBe true
        }

        "sendMoneyToOtherCountries is None" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None,
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123")),
            fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("456"))
          )
          await(service.isRenewalComplete(model)) mustBe true
        }
      }

    }

    "be false" when {

      "Standard renewal flow questions section are incomplete" when {

        "involvedInOtherActivities is not defined" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = None,
            hasAccepted = true
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "turnover is not defined" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = Some(InvolvedInOtherYes("test")),
            turnover = None,
            hasAccepted = true
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "if involvedInOtherActivities and businessTurnover is not defined" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = Some(InvolvedInOtherYes("test")),
            turnover = Some(AMLSTurnover.First),
            businessTurnover = None,
            hasAccepted = true
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "if not involvedInOtherActivities and businessTurnover is defined" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = Some(InvolvedInOtherNo),
            turnover = Some(AMLSTurnover.First),
            businessTurnover = Some(BusinessTurnover.First),
            hasAccepted = true
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "hasAccepted is false" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = Some(InvolvedInOtherYes("test")),
            turnover = Some(AMLSTurnover.First),
            businessTurnover = Some(BusinessTurnover.First),
            hasAccepted = false
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

      }

      "ASP is selected business activity and section is incomplete with standard renewal flow questions complete" when {

        "customerOutsideUk is not defined" in new ASPFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

      }

      "HVD is selected business activity and section is incomplete with standard renewal flow questions complete" when {

        "customersOutsideUk is not defined" in new HVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "customersOutsideUk is defined and percentageOfCashPaymentsOver15000 is not defined" in new HVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
            percentageOfCashPaymentOver15000 = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "customersOutsideUk is defined and percentageOfCashPaymentsOver15000 is defined and receivedCashPayments is not defined" in new HVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
            percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
            receiveCashPayments = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

      }

      "ASP and HVD are selected business activities and section is incomplete with standard renewal flow questions complete" when {

        "customersOutsideUk is not defined" in new ASPHVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "customersOutsideUk is defined and percentageOfCashPaymentsOver15000 is not defined" in new ASPHVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
            percentageOfCashPaymentOver15000 = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "customersOutsideUk is defined and percentageOfCashPaymentsOver15000 is defined and receivedCashPayments is not defined" in new ASPHVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
            percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
            receiveCashPayments = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

      }

      "MSB is selected business activity w/o MT, CE, FX subsectors and section is incomplete with standard renewal flow questions complete" when {
        "totalThroughput is not defined" in new MSBFixture {
          val model = preFilledModel.copy(
            totalThroughput = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }
      }

      "MSB is selected business activity with MT subsector and w/o CE, FX subsectors and section is incomplete with standard renewal flow questions complete" when {

        "totalThroughput is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is None and transactionsInLast12Months is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is None and transactionsInLast12Months is defined and mostTransactions is defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB"))))
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is false and transactionsInLast12Months is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is false and transactionsInLast12Months is defined and mostTransactions is defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB"))))
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months and mostTransactions is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months and mostTransactions is defined and sendTheLargestAmountsOfMoney is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            sendTheLargestAmountsOfMoney = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }
      }

      "MSB is selected business activity with CE subsector and w/o MT, FX subsectors and section is incomplete with standard renewal flow questions complete" when {
        "totalThroughput is not defined" in new CEFixture {
          val model = preFilledModel.copy(
            totalThroughput = None,
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123"))
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "whichCurrencies is not defined" in new CEFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            whichCurrencies = None,
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123"))
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "ceTransactionsInLast12Months is not defined" in new CEFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
            ceTransactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }
      }

      "MSB is selected business activity with FX subsector and w/o MT, CE subsectors and section is incomplete with standard renewal flow questions complete" when {
        "totalThroughput is not defined" in new FXFixture {
          val model = preFilledModel.copy(
            totalThroughput = None,
            fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("456"))
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "fxTransactionsInLast12Months is not defined" in new FXFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            fxTransactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }
      }

      "MSB is selected business activity with MT, CE, FX subsectors and section is incomplete with standard renewal flow questions complete" when {
        "totalThroughput is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is None and transactionsInLast12Months is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is None and transactionsInLast12Months is defined and mostTransactions is defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB"))))
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is false and transactionsInLast12Months is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is false and transactionsInLast12Months is defined and mostTransactions is defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB"))))
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months and mostTransactions is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months and mostTransactions is defined and sendTheLargestAmountsOfMoney is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            sendTheLargestAmountsOfMoney = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "whichCurrencies is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("us", "US"))),
            whichCurrencies = None,
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123"))
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "ceTransactionsInLast12Months is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("us", "US"))),
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
            ceTransactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }

        "fxTransactionsInLast12Months is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("us", "US"))),
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123")),
            fxTransactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model)) mustBe false
        }
      }
    }
  }

  trait CanSubmitFixture extends Fixture {
      val notStartedRenewal = Section("renewal", NotStarted, false, mock[Call])
      val startedRenewal = Section("renewal", Started, true, mock[Call])
      val completedUnchangedRenewal = Section("renewal", Completed, false, mock[Call])
      val completedChangedRenewal = Section("renewal", Completed, true, mock[Call])

      val sectionsCompletedAndChanged = Seq(
          Section("", Completed, false, mock[Call]),
          Section("", Completed, true, mock[Call])
      )

      val sectionCompletedAndNotChanged = Seq(
          Section("", Completed, false, mock[Call]),
          Section("", Completed, false, mock[Call])
      )

      val sectionsMutuallyIncompleteAndChanged = Seq(
          Section("", Started, false, mock[Call]),
          Section("", Completed, true, mock[Call])
      )

      val sectionIncompleteAndChanged = Seq(
          Section("", Started, true, mock[Call]),
          Section("", Completed, false, mock[Call])
      )

      val sectionsIncompleteAndNotChanged = Seq(
          Section("", Completed, false, mock[Call]),
          Section("", Started, false, mock[Call])
      )
  }

  "canSubmit" must {
    "return true" when {
      "renewal has not started" when {
        "sections are completed and changed" in new CanSubmitFixture {
          service.canSubmit(notStartedRenewal, sectionsCompletedAndChanged) must be(true)
        }
      }

      "renewal section is complete and changed" when {
        "sections are completed and changed" in new CanSubmitFixture {
          service.canSubmit(completedChangedRenewal, sectionsCompletedAndChanged) must be(true)
        }

        "sections are completed and not changed" in new CanSubmitFixture {
          service.canSubmit(completedChangedRenewal, sectionCompletedAndNotChanged) must be(true)
        }
      }

      "renewal section is complete and not changed" when {
        "sections are completed and changed" in new CanSubmitFixture {
          service.canSubmit(completedUnchangedRenewal, sectionsCompletedAndChanged) must be(true)
        }
      }

    }

    "return false" when {
      "renewal has started" when {

        "sections are completed and not changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Started, true, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Completed, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are completed and changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Started, true, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Completed, true, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are incomplete and changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Started, true, mock[Call])
          val sections = Seq(
            Section("", Started, false, mock[Call]),
            Section("", Completed, true, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are mutually incomplete and changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Started, true, mock[Call])
          val sections = Seq(
            Section("", Started, true, mock[Call]),
            Section("", Completed, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are incomplete and not changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Started, true, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Started, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }
      }

      "renewal has not started" when {

        "sections are completed and not changed" in new CanSubmitFixture {
          val renewal = Section("renewal", NotStarted, false, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Completed, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are incomplete and changed" in new CanSubmitFixture {
          val renewal = Section("renewal", NotStarted, true, mock[Call])
          val sections = Seq(
            Section("", Started, false, mock[Call]),
            Section("", Completed, true, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are mutually incomplete and changed" in new CanSubmitFixture {
          val renewal = Section("renewal", NotStarted, true, mock[Call])
          val sections = Seq(
            Section("", Started, true, mock[Call]),
            Section("", Completed, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are incomplete and not changed" in new CanSubmitFixture {
          val renewal = Section("renewal", NotStarted, true, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Started, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }
      }

      "renewal has completed and not changed" when {

        "sections are completed and not changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Completed, false, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Completed, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are incomplete and changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Completed, false, mock[Call])
          val sections = Seq(
            Section("", Started, false, mock[Call]),
            Section("", Completed, true, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are mutually incomplete and changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Completed, false, mock[Call])
          val sections = Seq(
            Section("", Started, true, mock[Call]),
            Section("", Completed, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are incomplete and not changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Completed, false, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Started, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }
      }

      "renewal has completed and changed" when {

        "sections are incomplete and changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Completed, true, mock[Call])
          val sections = Seq(
            Section("", Started, false, mock[Call]),
            Section("", Completed, true, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are mutually incomplete and changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Completed, true, mock[Call])
          val sections = Seq(
            Section("", Started, true, mock[Call]),
            Section("", Completed, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }

        "sections are incomplete and not changed" in new CanSubmitFixture {
          val renewal = Section("renewal", Completed, true, mock[Call])
          val sections = Seq(
            Section("", Completed, false, mock[Call]),
            Section("", Started, false, mock[Call])
          )

          service.canSubmit(renewal, sections) must be(false)
        }
      }
    }
  }
}
