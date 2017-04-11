package models.renewal

import models.{Country, SubscriptionRequest}
import models.businessactivities.BusinessActivities
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal.Conversions._
import org.scalatest.{MustMatchers, WordSpec}

class ConversionsSpec extends WordSpec with MustMatchers {

  trait Fixture {
    val businessActivities = BusinessActivities()
    val msbSection = MoneyServiceBusiness()
    val subscriptionRequest = SubscriptionRequest(None, None, None, None, None, None, Some(businessActivities), None, None, None, Some(msbSection), None, None)
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
      val model = MsbThroughput("03")
      val renewal = Renewal(msbThroughput = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.msbSection.get.throughput mustBe Some(models.moneyservicebusiness.ExpectedThroughput.Third)
    }

    "convert the 'MSB money transfers' model" in new Fixture {
      val model = MsbMoneyTransfers(2)
      val renewal = Renewal(msbTransfers = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.msbSection.get.transactionsInNext12Months mustBe Some(models.moneyservicebusiness.TransactionsInNext12Months)
    }

    "convert the 'MSB largest amounts' model" in new Fixture {
      val model = SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"), Some(Country("France", "FR")), Some(Country("us", "US")))
      val renewal = Renewal(sendTheLargestAmountsOfMoney = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.msbSection.get.sendTheLargestAmountsOfMoney mustBe Some(models.moneyservicebusiness.SendTheLargestAmountsOfMoney)
    }

    "convert the 'MSB most transactions' model" in new Fixture {
      val model = MostTransactions(Seq(Country("United Kingdom", "GB")))
      val renewal = Renewal(mostTransactions = Some(model))
      val converted = subscriptionRequest.withRenewalData((renewal))

      converted.msbSection.get.mostTransactions mustBe Some(models.moneyservicebusiness.MostTransactions)
    }

    "convert the 'MSB currency transactions' model" in new Fixture {
      val model = CETransactions("12345678963")
      val renewal = Renewal(ceTransactions = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.msbSection.get.ceTransactionsInNext12Months mustBe Some(models.moneyservicebusiness.CETransactionsInNext12Months)
    }

    "convert the 'MSB which currencies' model" in new Fixture{

    }

    "convert the 'HVD percentage' model" in new Fixture {
      val model = PercentageOfCashPaymentOver15000.First
      val renewal = Renewal(percentageOfCashPaymentOver15000 = Some(model))
      val converted = subscriptionRequest.withRenewalData(renewal)

      converted.hvdSection.get.percentageOfCashPaymentOver15000 mustBe Some(models.hvd.PercentageOfCashPaymentOver15000)
    }

    "convert the 'HVD receive cash payments' model" in new Fixture {
      val model = ReceiveCashPayments(Some(PaymentMethods(true,true,Some("other"))))
      val renewal = Renewal(ReceiveCashPayments = Some(model))

    }



  }

}
