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

package models.renewal

import models.{Country, SubscriptionRequest}
import models.businessactivities.BusinessActivities
import models.hvd.Hvd
import models.moneyservicebusiness.{BankMoneySource, MoneyServiceBusiness, MoneySources, UsesForeignCurrenciesNo}
import models.renewal.Conversions._
import org.scalatest.{MustMatchers, WordSpec}

class ConversionsSpec extends WordSpec with MustMatchers {

  trait Fixture {
    val businessActivities = BusinessActivities()
    val msbSection = MoneyServiceBusiness()
    val hvdSection = Hvd()
    val subscriptionRequest = SubscriptionRequest(None, None, None, None, None, None, Some(businessActivities), None, None, None, Some(msbSection), Some(hvdSection), None)
  }

  "The renewal converter" must {

    "convert the AMLS expected turnover" in new Fixture {
      val turnover: AMLSTurnover = AMLSTurnover.First
      val renewal = Renewal(turnover = Some(turnover))

      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.businessActivitiesSection.get.expectedAMLSTurnover mustBe Some(models.businessactivities.ExpectedAMLSTurnover.First)
    }

    "convert the business turnover" in new Fixture {
      val businessTurnover: BusinessTurnover = BusinessTurnover.Second
      val renewal = Renewal(businessTurnover = Some(businessTurnover))

      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.businessActivitiesSection.get.expectedBusinessTurnover mustBe Some(models.businessactivities.ExpectedBusinessTurnover.Second)
    }

    "convert the 'involved in other businesses' model" in new Fixture {
      val model: InvolvedInOther = InvolvedInOtherYes("some other business")
      val renewal = Renewal(involvedInOtherActivities = Some(model))

      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.businessActivitiesSection.get.involvedInOther mustBe Some(models.businessactivities.InvolvedInOtherYes("some other business"))
    }

    "convert the 'customers outside the UK' model" in new Fixture {
      val country = Country("My Country", "MC")
      val model = CustomersOutsideUK(Some(Seq(country)))
      val renewal = Renewal(customersOutsideUK = Some(model))

      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.businessActivitiesSection.get.customersOutsideUK mustBe Some(models.businessactivities.CustomersOutsideUK(Some(Seq(country))))
    }

    "convert the 'MSB throughput' model" in new Fixture {
      val model = TotalThroughput("03")
      val renewal = Renewal(totalThroughput = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.msbSection.get.throughput mustBe Some(models.moneyservicebusiness.ExpectedThroughput.Third)
    }

    "convert the 'MSB money transfers' model" in new Fixture {
      val model = TransactionsInLast12Months("2500")
      val renewal = Renewal(transactionsInLast12Months = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.msbSection.get.transactionsInNext12Months mustBe Some(models.moneyservicebusiness.TransactionsInNext12Months("2500"))
    }

    "convert the 'MSB largest amounts' model" in new Fixture {
      val model = SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"), Some(Country("France", "FR")), Some(Country("us", "US")))
      val renewal = Renewal(sendTheLargestAmountsOfMoney = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.msbSection.get.sendTheLargestAmountsOfMoney mustBe Some(
        models.moneyservicebusiness.SendTheLargestAmountsOfMoney(
          Country("United Kingdom", "GB"), Some(Country("France", "FR")), Some(Country("us", "US"))))
    }

    "convert the 'MSB most transactions' model" in new Fixture {
      val model = MostTransactions(Seq(Country("United Kingdom", "GB")))
      val renewal = Renewal(mostTransactions = Some(model))
      val converted = subscriptionRequest.withRenewalData((renewal))

      converted.msbSection.get.mostTransactions mustBe Some(models.moneyservicebusiness.MostTransactions(Seq(Country("United Kingdom", "GB"))))
    }

    "convert the 'MSB currency transactions' model" in new Fixture {
      val model = CETransactionsInLast12Months("12345678963")
      val renewal = Renewal(ceTransactionsInLast12Months = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.msbSection.get.ceTransactionsInNext12Months mustBe Some(models.moneyservicebusiness.CETransactionsInNext12Months("12345678963"))
    }

    "convert the 'MSB which currencies' model" in new Fixture {
      val model = WhichCurrencies(Seq("USD", "CHF", "EUR"), None, Some(BankMoneySource("Bank names")), None, None)
      val renewal = Renewal(whichCurrencies = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.msbSection.get.whichCurrencies mustBe Some(
        models.moneyservicebusiness.WhichCurrencies(
          currencies = Seq("USD", "CHF", "EUR"),
          usesForeignCurrencies = Some(UsesForeignCurrenciesNo),
          moneySources = Some(MoneySources(Some(BankMoneySource("Bank names"))))
        ))

    }

    "convert the 'MSB foreign exchange transactions' model" in new Fixture {
      val model = FXTransactionsInLast12Months("987")
      val renewal = Renewal(fxTransactionsInLast12Months = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.msbSection.get.fxTransactionsInNext12Months mustBe Some(
        models.moneyservicebusiness.FXTransactionsInNext12Months("987")
      )
    }

    "convert the 'HVD percentage' model" in new Fixture {
      val model = PercentageOfCashPaymentOver15000.First
      val renewal = Renewal(percentageOfCashPaymentOver15000 = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.hvdSection.get.percentageOfCashPaymentOver15000 mustBe Some(models.hvd.PercentageOfCashPaymentOver15000.First)
    }

    "convert the 'HVD receive cash payments' model" in new Fixture {

      val model = ReceiveCashPayments(Some(PaymentMethods(true,true,Some("other"))))
      val renewal = Renewal(receiveCashPayments = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.hvdSection.get.receiveCashPayments mustBe Some(true)
      converted.hvdSection.get.cashPaymentMethods mustBe Some(models.hvd.PaymentMethods(true,true,Some("other")))

    }

  }

}
