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

package services

import connectors.DataCacheConnector
import models.Country
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService._
import models.businessmatching._
import models.registrationprogress._
import models.renewal._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{Format, Json}
import play.api.test.Helpers._
import services.RenewalService.BusinessAndOtherActivities
import uk.gov.hmrc.http.HeaderCarrier
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future


class RenewalServiceSpec extends AmlsSpec with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Fixture {

    val dataCache = mock[DataCacheConnector]
    val statusService = mock[StatusService]

    val service = new RenewalService(dataCache, statusService)

    val credId = "12345678"

    val mockCacheMap = mock[Cache]

    when(dataCache.fetchAll(any()))
      .thenReturn(Future.successful(Some(mockCacheMap)))
    when(mockCacheMap.getEntry[BusinessMatching](any())(any()))
      .thenReturn(Some(BusinessMatching()))

    def setupBusinessMatching(activities: Set[BusinessActivity] = Set(), msbServices: Set[BusinessMatchingMsbService] = Set()) = when {
      mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(msbServices)), activities = Some(BusinessActivities(activities))))

    def setUpRenewal(renewalModel: Renewal) = when {
      dataCache.fetch[Renewal](any(), eqTo(Renewal.key))(any())
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
          dataCache.fetch[Renewal](any(), eqTo(Renewal.key))(any())
        } thenReturn Future.successful(None)

        val section = await(service.getTaskRow(credId))
        section mustBe TaskRow(
          Renewal.sectionKey,
          controllers.renewal.routes.WhatYouNeedController.get.url,
          false,
          NotStarted,
          TaskRow.notStartedTag
        )
      }

      "the renewal is complete and has been started" in new Fixture {
        setupBusinessMatching(Set(MoneyServiceBusiness, HighValueDealing), Set(CurrencyExchange, TransmittingMoney))

        val completeModel = Renewal(
          Some(InvolvedInOtherYes("test")),
          Some(BusinessTurnover.First),
          Some(AMLSTurnover.First),
          Some(AMPTurnover.First),
          Some(CustomersOutsideIsUK(true)),
          Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          Some(PercentageOfCashPaymentOver15000.First),
          Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other")))))),
          Some(TotalThroughput("01")),
          Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
          Some(TransactionsInLast12Months("1500")),
          Some(SendTheLargestAmountsOfMoney(Seq(Country("us", "US")))),
          Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
          Some(CETransactionsInLast12Months("123")),
          Some(FXTransactionsInLast12Months("456")),
          true,
          Some(SendMoneyToOtherCountry(true))
        )

        setUpRenewal(completeModel)

        val taskRow = await(service.getTaskRow(credId))
        await(service.isRenewalComplete(completeModel, credId)) mustBe true
        taskRow mustBe TaskRow(
          Renewal.sectionKey,
          controllers.renewal.routes.SummaryController.get.url,
          true,
          Completed,
          TaskRow.completedTag
        )
      }

      "the renewal model is not complete" in new Fixture {
        val renewal = mock[Renewal]
        when(renewal.hasChanged) thenReturn true

        setUpRenewal(renewal)

        val taskRow = await(service.getTaskRow(credId))
        taskRow mustBe TaskRow(
          Renewal.sectionKey,
          controllers.renewal.routes.WhatYouNeedController.get.url,
          true,
          Started,
          TaskRow.incompleteTag
        )
      }

      "the renewal model is not complete and not started" in new Fixture {
        val renewal = Renewal(None)
        setUpRenewal(renewal)

        val taskRow = await(service.getTaskRow(credId))
        taskRow mustBe TaskRow(
          Renewal.sectionKey,
          controllers.renewal.routes.WhatYouNeedController.get.url,
          false,
          NotStarted,
          TaskRow.notStartedTag
        )
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
          await(service.isRenewalComplete(model, credId)) mustBe true
        }

        "involvedInOtherActivites is false" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = Some(InvolvedInOtherNo),
            turnover = Some(AMLSTurnover.First),
            businessTurnover = None,
            hasAccepted = true
          )
          await(service.isRenewalComplete(model, credId)) mustBe true
        }
      }

      "ASP is selected business activity and section is complete along with standard renewal flow questions" in new ASPFixture {
        val model = preFilledModel.copy(
          customersOutsideIsUK = Some(CustomersOutsideIsUK(true)),
          customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB")))))
        )
        await(service.isRenewalComplete(model, credId)) mustBe true
      }

      "HVD is selected business activity and section is complete along with standard renewal flow questions" in new HVDFixture {
        val model = preFilledModel.copy(
          customersOutsideIsUK = Some(CustomersOutsideIsUK(true)),
          customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
          receiveCashPayments = Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other"))))))

        )
        await(service.isRenewalComplete(model, credId)) mustBe true
      }

      "ASP and HVD are selected business activities and section is complete along with standard renewal flow questions" in new ASPHVDFixture {
        val model = preFilledModel.copy(
          customersOutsideIsUK = Some(CustomersOutsideIsUK(true)),
          customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
          receiveCashPayments = Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other"))))))
        )
        await(service.isRenewalComplete(model, credId)) mustBe true
      }

      "MSB is selected business activity w/o MT, CE, FX subsectors and section is complete along with standard renewal flow questions" in new MSBFixture {
        val model = preFilledModel.copy(
          totalThroughput = Some(TotalThroughput("01"))
        )
        await(service.isRenewalComplete(model, credId)) mustBe true
      }

      "MSB is selected business activity with MT subsector and w/o CE, FX subsectors and section is complete along with standard renewal flow questions" when {
        "sendMoneyToOtherCountries is true" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(Country("us", "US")))),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB"))))
          )
          await(service.isRenewalComplete(model, credId)) mustBe true
        }

        "sendMoneyToOtherCountries is false" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe true
        }

        "sendMoneyToOtherCountries is None" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe true
        }
      }

      "MSB is selected business activity with CE subsector and w/o MT, FX subsectors and section is complete along with standard renewal flow questions" in new CEFixture {
        val model = preFilledModel.copy(
          totalThroughput = Some(TotalThroughput("01")),
          whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
          ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123"))
        )
        await(service.isRenewalComplete(model, credId)) mustBe true
      }

      "MSB is selected business activity with FX subsector and w/o MT, CE subsectors and section is complete along with standard renewal flow questions" in new FXFixture {
        val model = preFilledModel.copy(
          totalThroughput = Some(TotalThroughput("01")),
          fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("456"))
        )
        await(service.isRenewalComplete(model, credId)) mustBe true
      }

      "MSB is selected business activity with MT, CE, FX subsectors and section is complete along with standard renewal flow questions" when {
        "sendMoneyToOtherCountries is true" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(Country("us", "US")))),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123")),
            fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("456"))
          )
          await(service.isRenewalComplete(model, credId)) mustBe true
        }

        "sendMoneyToOtherCountries is False" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None,
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123")),
            fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("456"))
          )
          await(service.isRenewalComplete(model, credId)) mustBe true
        }

        "sendMoneyToOtherCountries is None" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None,
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123")),
            fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("456"))
          )
          await(service.isRenewalComplete(model, credId)) mustBe true
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
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "turnover is not defined" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = Some(InvolvedInOtherYes("test")),
            turnover = None,
            hasAccepted = true
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "if involvedInOtherActivities and businessTurnover is not defined" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = Some(InvolvedInOtherYes("test")),
            turnover = Some(AMLSTurnover.First),
            businessTurnover = None,
            hasAccepted = true
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "if not involvedInOtherActivities and businessTurnover is defined" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = Some(InvolvedInOtherNo),
            turnover = Some(AMLSTurnover.First),
            businessTurnover = Some(BusinessTurnover.First),
            hasAccepted = true
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "hasAccepted is false" in new StandardFixture {
          val model = preFilledModel.copy(
            involvedInOtherActivities = Some(InvolvedInOtherYes("test")),
            turnover = Some(AMLSTurnover.First),
            businessTurnover = Some(BusinessTurnover.First),
            hasAccepted = false
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

      }

      "ASP is selected business activity and section is incomplete with standard renewal flow questions complete" when {

        "customerOutsideUk is not defined" in new ASPFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

      }

      "HVD is selected business activity and section is incomplete with standard renewal flow questions complete" when {

        "customersOutsideUk is not defined" in new HVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "customersOutsideUk is defined and percentageOfCashPaymentsOver15000 is not defined" in new HVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
            percentageOfCashPaymentOver15000 = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "customersOutsideUk is defined and percentageOfCashPaymentsOver15000 is defined and receivedCashPayments is not defined" in new HVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
            percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
            receiveCashPayments = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "customersOutsideUk is defined and percentageOfCashPaymentsOver15000 is defined and receivedCashPayments is defined" when {
          "CashPaymentsCustomerNotMet is true but payments missing" in new HVDFixture {
            val model = preFilledModel.copy(
              customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
              percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
              receiveCashPayments = Some(CashPayments(CashPaymentsCustomerNotMet(true), None))
            )
            await(service.isRenewalComplete(model, credId)) mustBe false
          }
        }
      }

      "ASP and HVD are selected business activities and section is incomplete with standard renewal flow questions complete" when {

        "customersOutsideUk is not defined" in new ASPHVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "customersOutsideUk is defined and percentageOfCashPaymentsOver15000 is not defined" in new ASPHVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
            percentageOfCashPaymentOver15000 = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "customersOutsideUk is defined and percentageOfCashPaymentsOver15000 is defined and receivedCashPayments is not defined" in new ASPHVDFixture {
          val model = preFilledModel.copy(
            customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
            percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
            receiveCashPayments = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

      }

      "MSB is selected business activity w/o MT, CE, FX subsectors and section is incomplete with standard renewal flow questions complete" when {
        "totalThroughput is not defined" in new MSBFixture {
          val model = preFilledModel.copy(
            totalThroughput = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }
      }

      "MSB is selected business activity with MT subsector and w/o CE, FX subsectors and section is incomplete with standard renewal flow questions complete" when {

        "totalThroughput is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is None and transactionsInLast12Months is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is None and transactionsInLast12Months is defined and mostTransactions is defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB"))))
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is false and transactionsInLast12Months is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is false and transactionsInLast12Months is defined and mostTransactions is defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB"))))
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months and mostTransactions is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months and mostTransactions is defined and sendTheLargestAmountsOfMoney is not defined" in new MTFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            sendTheLargestAmountsOfMoney = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }
      }

      "MSB is selected business activity with CE subsector and w/o MT, FX subsectors and section is incomplete with standard renewal flow questions complete" when {
        "totalThroughput is not defined" in new CEFixture {
          val model = preFilledModel.copy(
            totalThroughput = None,
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123"))
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "whichCurrencies is not defined" in new CEFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            whichCurrencies = None,
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123"))
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "ceTransactionsInLast12Months is not defined" in new CEFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
            ceTransactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }
      }

      "MSB is selected business activity with FX subsector and w/o MT, CE subsectors and section is incomplete with standard renewal flow questions complete" when {
        "totalThroughput is not defined" in new FXFixture {
          val model = preFilledModel.copy(
            totalThroughput = None,
            fxTransactionsInLast12Months = Some(FXTransactionsInLast12Months("456"))
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "fxTransactionsInLast12Months is not defined" in new FXFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            fxTransactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }
      }

      "MSB is selected business activity with MT, CE, FX subsectors and section is incomplete with standard renewal flow questions complete" when {
        "totalThroughput is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is None and transactionsInLast12Months is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is None and transactionsInLast12Months is defined and mostTransactions is defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = None,
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB"))))
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is false and transactionsInLast12Months is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is false and transactionsInLast12Months is defined and mostTransactions is defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB"))))
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months and mostTransactions is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "sendMoneyToOtherCountries is true and transactionsInLast12Months and mostTransactions is defined and sendTheLargestAmountsOfMoney is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            sendTheLargestAmountsOfMoney = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "whichCurrencies is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(Country("us", "US")))),
            whichCurrencies = None,
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123"))
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "ceTransactionsInLast12Months is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(Country("us", "US")))),
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
            ceTransactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }

        "fxTransactionsInLast12Months is not defined" in new AllFixture {
          val model = preFilledModel.copy(
            totalThroughput = Some(TotalThroughput("01")),
            sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
            transactionsInLast12Months = Some(TransactionsInLast12Months("1500")),
            mostTransactions = Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
            sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(Country("us", "US")))),
            whichCurrencies = Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
            ceTransactionsInLast12Months = Some(CETransactionsInLast12Months("123")),
            fxTransactionsInLast12Months = None
          )
          await(service.isRenewalComplete(model, credId)) mustBe false
        }
      }
    }
  }

  trait CanSubmitFixture extends Fixture {
    val notStartedRenewal = TaskRow("renewal", "/foo", false, NotStarted, TaskRow.notStartedTag)
    val startedRenewal = TaskRow("renewal", "/foo", true, Started, TaskRow.incompleteTag)
    val completedUnchangedRenewal = TaskRow("renewal", "/foo", false, Completed, TaskRow.completedTag)
    val completedChangedRenewal = TaskRow("renewal", "/foo", true, Completed, TaskRow.completedTag)
    val updatedChangedRenewal = TaskRow("renewal", "/foo", true, Updated, TaskRow.updatedTag)

    val sectionsCompletedAndUpdated = Seq(
      TaskRow("", "/foo", false, Completed, TaskRow.completedTag),
      TaskRow("", "/foo", true, Updated, TaskRow.updatedTag)
    )
    val sectionsCompletedAndChanged = Seq(
      TaskRow("", "/foo", false, Completed, TaskRow.completedTag),
      TaskRow("", "/foo", true, Completed, TaskRow.completedTag)
    )

    val sectionCompletedAndNotChanged = Seq(
      TaskRow("", "/foo", false, Completed, TaskRow.completedTag),
      TaskRow("", "/foo", false, Completed, TaskRow.completedTag)
    )

    val sectionsMutuallyIncompleteAndChanged = Seq(
      TaskRow("", "/foo", false, Started, TaskRow.incompleteTag),
      TaskRow("", "/foo", true, Completed, TaskRow.completedTag)
    )

    val sectionIncompleteAndChanged = Seq(
      TaskRow("", "/foo", true, Started, TaskRow.incompleteTag),
      TaskRow("", "/foo", false, Completed, TaskRow.completedTag)
    )

    val sectionsIncompleteAndNotChanged = Seq(
      TaskRow("", "/foo", false, Completed, TaskRow.completedTag),
      TaskRow("", "/foo", false, Started, TaskRow.incompleteTag)
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

      "sections are updated only" in new CanSubmitFixture {
        service.canSubmit(completedUnchangedRenewal, Seq(updatedChangedRenewal)) must be(true)
      }

      "sections are a combination of completed and updated" in new CanSubmitFixture {
        service.canSubmit(completedUnchangedRenewal, sectionsCompletedAndUpdated) must be(true)
      }
    }

    "return false" when {
      "renewal has started" when {

        "sections are completed and not changed" in new CanSubmitFixture {
          service.canSubmit(startedRenewal, sectionCompletedAndNotChanged) must be(false)
        }

        "sections are completed and changed" in new CanSubmitFixture {
          service.canSubmit(startedRenewal, sectionsCompletedAndChanged) must be(false)
        }

        "sections are incomplete and changed" in new CanSubmitFixture {
          service.canSubmit(startedRenewal, sectionIncompleteAndChanged) must be(false)
        }

        "sections are mutually incomplete and changed" in new CanSubmitFixture {
          service.canSubmit(startedRenewal, sectionsMutuallyIncompleteAndChanged) must be(false)
        }

        "sections are incomplete and not changed" in new CanSubmitFixture {
          service.canSubmit(startedRenewal, sectionsIncompleteAndNotChanged) must be(false)
        }
      }

      "renewal has not started" when {

        "sections are completed and not changed" in new CanSubmitFixture {
          service.canSubmit(notStartedRenewal, sectionCompletedAndNotChanged) must be(false)
        }

        "sections are incomplete and changed" in new CanSubmitFixture {
          service.canSubmit(notStartedRenewal, sectionIncompleteAndChanged) must be(false)
        }

        "sections are mutually incomplete and changed" in new CanSubmitFixture {
          service.canSubmit(notStartedRenewal, sectionsMutuallyIncompleteAndChanged) must be(false)
        }

        "sections are incomplete and not changed" in new CanSubmitFixture {
          service.canSubmit(notStartedRenewal, sectionsIncompleteAndNotChanged) must be(false)
        }
      }

      "renewal has completed and not changed" when {

        "sections are completed and not changed" in new CanSubmitFixture {
          service.canSubmit(completedUnchangedRenewal, sectionCompletedAndNotChanged) must be(false)
        }

        "sections are incomplete and changed" in new CanSubmitFixture {
          service.canSubmit(completedUnchangedRenewal, sectionIncompleteAndChanged) must be(false)
        }

        "sections are mutually incomplete and changed" in new CanSubmitFixture {
          service.canSubmit(completedUnchangedRenewal, sectionsMutuallyIncompleteAndChanged) must be(false)
        }

        "sections are incomplete and not changed" in new CanSubmitFixture {
          service.canSubmit(completedUnchangedRenewal, sectionsIncompleteAndNotChanged) must be(false)
        }
      }

      "renewal has completed and changed" when {

        "sections are incomplete and changed" in new CanSubmitFixture {
          service.canSubmit(completedChangedRenewal, sectionIncompleteAndChanged) must be(false)
        }

        "sections are mutually incomplete and changed" in new CanSubmitFixture {
          service.canSubmit(completedChangedRenewal, sectionsMutuallyIncompleteAndChanged) must be(false)
        }

        "sections are incomplete and not changed" in new CanSubmitFixture {
          service.canSubmit(completedChangedRenewal, sectionsIncompleteAndNotChanged) must be(false)
        }
      }
    }
  }

  "getFirstBusinessActivityInLowercase" must {

    "return an activity" when {

      "the length of the activities list is exactly 1" in new Fixture {

        BusinessActivities.all foreach { activity =>
          when {
            dataCache.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any())
          } thenReturn Future.successful(
            Some(BusinessMatching(activities = Some(BusinessActivities(Set(activity)))))
          )

          service.getFirstBusinessActivityInLowercase(credId).futureValue mustBe Some(
            messages(s"businessactivities.registerservices.servicename.lbl.${activity.value}")
          )
        }
      }
    }

    "return none" when {

      "the length of the activities is zero" in new Fixture {

        when {
          dataCache.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any())
        } thenReturn Future.successful(
          Some(BusinessMatching(activities = Some(BusinessActivities(Set.empty))))
        )

        service.getFirstBusinessActivityInLowercase(credId).futureValue mustBe None
      }

      "the length of the activities is longer than 1" in new Fixture {

        when {
          dataCache.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any())
        } thenReturn Future.successful(
          Some(BusinessMatching(activities = Some(BusinessActivities(Set(AccountancyServices, ArtMarketParticipant)))))
        )

        service.getFirstBusinessActivityInLowercase(credId).futureValue mustBe None
      }

      "no activities are returned" in new Fixture {

        when {
          dataCache.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any())
        } thenReturn Future.successful(None)

        service.getFirstBusinessActivityInLowercase(credId).futureValue mustBe None
      }
    }
  }

  "getBusinessMatching" must {

    "return business matching instance" when {

      "cache connector retrieves an instance successfully" in new Fixture {

        val bm: BusinessMatching = BusinessMatching()

        when {
          dataCache.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any())
        } thenReturn Future.successful(Some(bm))

        service.getBusinessMatching(credId).futureValue mustBe Some(bm)
      }
    }

    "return none" when {

      "cache connector cannot retrieve business matching" in new Fixture {

        when {
          dataCache.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any())
        } thenReturn Future.successful(None)

        service.getBusinessMatching(credId).futureValue mustBe None
      }
    }
  }

  "fetchAndUpdateRenewal" must {

    "return the updated cachemap" when {

      "renewal is updated correctly" in new Fixture {

        val model = standardCompleteInvolvedInOtherActivities()

        when {
          dataCache.fetch[Renewal](any(), any())(any())
        } thenReturn Future.successful(Some(model))

        when {
          dataCache.save(eqTo(credId), eqTo(Renewal.sectionKey), any())(any())
        } thenReturn Future.successful(mockCacheMap)

        val result = service.fetchAndUpdateRenewal(
          credId,
          renewal => renewal.copy(hasAccepted = false)
        ).futureValue

        val captor = ArgumentCaptor.forClass(classOf[Renewal])
        verify(dataCache).save(eqTo(credId), eqTo(Renewal.sectionKey), captor.capture())(any())

        result mustBe Some(mockCacheMap)
        captor.getValue mustBe model.copy(hasAccepted = false)
      }
    }

    "return None" when {

      "renewal is not present in cache" in new Fixture {

        when {
          dataCache.fetch[Renewal](any(), any())(any())
        } thenReturn Future.successful(None)

        val result = service.fetchAndUpdateRenewal(
          credId,
          renewal => renewal.copy(hasAccepted = false)
        ).futureValue

        result mustBe None
      }
    }
  }

  "createOrUpdate" must {
    "insert a new renewal when there isn't one" in {
      // Given
      val cache = Cache("test", Map(Renewal.key -> Json.toJson(Renewal())))
      val cacheId = "123456"
      val dataCache = mock[DataCacheConnector]
      when(dataCache.fetch[Renewal](eqTo(cacheId), eqTo(Renewal.key))(any[Format[Renewal]]))
        .thenReturn(Future.successful(None))
      when(dataCache.save[Renewal](eqTo(cacheId), eqTo(Renewal.key), eqTo(Renewal()))(any[Format[Renewal]]))
        .thenReturn(Future.successful(cache))

      val statusService = mock[StatusService]

      // When
      val service = new RenewalService(dataCache, statusService)
      val updatedCache = service.createOrUpdateRenewal(cacheId, r => r.copy(involvedInOtherActivities = None), Renewal()).futureValue

      // Then
      updatedCache.getEntry(Renewal.key)(Renewal.jsonReads).value mustBe Renewal(customersOutsideUK = Some(CustomersOutsideUK(None)))
    }

    "update an existing renewal when there is one" in {
      // Given
      val cache = Cache("test", Map(Renewal.key -> Json.toJson(Renewal(Some(InvolvedInOtherYes("selling software packages"))))))
      val cacheId = "123456"
      val dataCache = mock[DataCacheConnector]
      when(dataCache.fetch[Renewal](eqTo(cacheId), eqTo(Renewal.key))(any[Format[Renewal]]))
        .thenReturn(Future.successful(Some(Renewal(Some(InvolvedInOtherNo)))))
      when(dataCache
        .save[Renewal](eqTo(cacheId), eqTo(Renewal.key), eqTo(Renewal(Some(InvolvedInOtherYes("selling software packages")))))(any[Format[Renewal]]))
        .thenReturn(Future.successful(cache))
      val statusService = mock[StatusService]
      // When
      val renewalService = new RenewalService(dataCache, statusService)
      val updatedCache = renewalService.createOrUpdateRenewal(
        cacheId,
        r => r.copy(involvedInOtherActivities = Some(InvolvedInOtherYes("selling software packages"))),
        Renewal()
      ).futureValue

      // Then
      updatedCache.getEntry(Renewal.key)(Renewal.jsonReads).value mustBe
        Renewal(
          involvedInOtherActivities = Some(InvolvedInOtherYes("selling software packages")),
          customersOutsideUK = Some(CustomersOutsideUK(None))
        )
    }
  }

  "updateOtherBusinessActivities" must {
    "return business and other activities" when {
      "there is an existing renewal & business matching" in {
        // Given
        val cacheId = "123456"
        val dataCache = mock[DataCacheConnector]
        when(dataCache.fetch[Renewal](eqTo(cacheId), eqTo(Renewal.key))(any[Format[Renewal]]))
          .thenReturn(Future.successful(Some(Renewal(Some(InvolvedInOtherNo)))))
        when(dataCache.fetch[BusinessMatching](eqTo(cacheId), eqTo(BusinessMatching.key))(any[Format[BusinessMatching]]))
          .thenReturn(Future.successful(Some(BusinessMatching(activities = Some(BusinessActivities(Set(AccountancyServices, MoneyServiceBusiness)))))))

        val statusService = mock[StatusService]

        // When
        val renewalService = new RenewalService(dataCache, statusService)
        val businessOtherActivities = renewalService.updateOtherBusinessActivities(cacheId, InvolvedInOtherYes("selling software packages"))
          .futureValue
          .value

        // Then
        businessOtherActivities mustBe BusinessAndOtherActivities(Set(AccountancyServices, MoneyServiceBusiness), InvolvedInOtherYes("selling software packages"))
      }

      "there is no renewal" in {
        // Given
        val cacheId = "123456"
        val dataCache = mock[DataCacheConnector]
        when(dataCache.fetch[Renewal](eqTo(cacheId), eqTo(Renewal.key))(any[Format[Renewal]])).thenReturn(Future.successful(None))
        when(dataCache.fetch[BusinessMatching](eqTo(cacheId), eqTo(BusinessMatching.key))(any[Format[BusinessMatching]]))
          .thenReturn(Future.successful(Some(BusinessMatching(activities = Some(BusinessActivities(Set(AccountancyServices, MoneyServiceBusiness)))))))
        val statusService = mock[StatusService]

        // When
        val renewalService = new RenewalService(dataCache, statusService)
        val businessOtherActivities = renewalService.updateOtherBusinessActivities(cacheId, InvolvedInOtherYes("trading")).futureValue.value

        // Then
        businessOtherActivities mustBe BusinessAndOtherActivities(Set(AccountancyServices, MoneyServiceBusiness), InvolvedInOtherYes("trading"))
      }
    }

    "return nothing" when {
      "there is no business matching" in {
        // Given
        val cacheId = "123456"
        val dataCache = mock[DataCacheConnector]
        when(dataCache.fetch[Renewal](eqTo(cacheId), eqTo(Renewal.key))(any[Format[Renewal]]))
          .thenReturn(Future.successful((Some(Renewal(Some(InvolvedInOtherNo))))))
        when(dataCache.fetch[BusinessMatching](eqTo(cacheId), eqTo(BusinessMatching.key))(any[Format[BusinessMatching]]))
          .thenReturn(Future.successful(None))

        val statusService = mock[StatusService]
        // When
        val renewalService = new RenewalService(dataCache, statusService)
        val businessOtherActivities = renewalService.updateOtherBusinessActivities(cacheId, InvolvedInOtherYes("trading")).futureValue

        // Then
        businessOtherActivities mustBe None
      }
    }
  }
}
