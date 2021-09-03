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

package models.renewal

import models.Country
import play.api.libs.json.{JsSuccess, Json}
import utils.AmlsSpec

import scala.collection.Seq

class RenewalSpec extends AmlsSpec {

  "The Renewal model" must {
    "succesfully validate if model is complete" when {
      "json is complete" in {
        val completeRenewal = Renewal(customersOutsideIsUK = Some(CustomersOutsideIsUK(true)),
          customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))))

        val json = Json.obj(
          "customersOutsideIsUK" -> Json.obj(
            "isOutside" -> true
          ),
          "customersOutsideUK" -> Json.obj(
            "countries" -> Seq("GB")
          ),
          "hasChanged" -> false,
          "hasAccepted" -> true
        )

        json.as[Renewal] must be(completeRenewal)
      }
    }

    "succesfully validate json" when {
      "CustomersOutsideIsUK is false" in {
        val renewal = Renewal(customersOutsideIsUK = Some(CustomersOutsideIsUK(false)), customersOutsideUK = Some(CustomersOutsideUK(None)))

        val json = Json.obj(
          "customersOutsideUK" -> Json.obj(
            "isOutside" -> false
          ),
          "hasChanged" -> false,
          "hasAccepted" -> true
        )

        json.as[Renewal] must be(renewal)
      }
    }

    "serialize to and from JSON" in {

     val completeRenewal = Renewal(
        Some(InvolvedInOtherYes("test")),
        Some(BusinessTurnover.First),
        Some(AMLSTurnover.First),
        Some(AMPTurnover.First),
        Some(CustomersOutsideIsUK(false)),
        Some(CustomersOutsideUK(None)),
        Some(PercentageOfCashPaymentOver15000.First),
        Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true,true,Some("other")))))),
        Some(TotalThroughput("01")),
        Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
        Some(TransactionsInLast12Months("1500")),
        Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
        Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
        Some(CETransactionsInLast12Months("123")),
        hasChanged = true
      )

      val json = Json.parse("""
         {"involvedInOtherActivities":
         {"involvedInOther":true,"details":"test"},
         "businessTurnover":{"businessTurnover":"01"},
         "turnover":{"turnover":"01"},
         "ampTurnover":{"percentageExpectedTurnover":"01"},
         "customersOutsideIsUK":{"isOutside":false},
         "percentageOfCashPaymentOver15000":{"percentage":"01"},
         "receiveCashPayments":{"receivePayments":true,"paymentMethods":{"courier":true,"direct":true,"other":true,"details":"other"}},
         "totalThroughput":{"throughput":"01"},
         "whichCurrencies":{"currencies":["EUR"],
         "usesForeignCurrencies":null,"moneySources":{}},
         "transactionsInLast12Months":{"transfers":"1500"},
         "sendTheLargestAmountsOfMoney":{"country_1":"GB"},
         "mostTransactions":{"mostTransactionsCountries":["GB"]},
         "ceTransactionsInLast12Months":{"ceTransaction":"123"},
         "hasChanged":true,
         "hasAccepted":true}
          """.stripMargin)

      Json.fromJson[Renewal](json) mustBe JsSuccess(completeRenewal)
    }

    "roundtrip through json" in {

      val completeRenewal = Renewal(
        Some(InvolvedInOtherYes("test")),
        Some(BusinessTurnover.First),
        Some(AMLSTurnover.First),
        Some(AMPTurnover.First),
        Some(CustomersOutsideIsUK(false)),
        Some(CustomersOutsideUK(Option(Nil))),
        Some(PercentageOfCashPaymentOver15000.First),
        Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true,true,Some("other")))))),
        Some(TotalThroughput("01")),
        Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
        Some(TransactionsInLast12Months("1500")),
        Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
        Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
        Some(CETransactionsInLast12Months("123")),
        hasChanged = true
      )

      Json.fromJson[Renewal](Json.toJson(completeRenewal)) mustEqual JsSuccess(completeRenewal)
    }

  }

  "successfully validate json" when {
    "receiveCashPayments is false" in {
      val renewal = Renewal(
        customersOutsideUK = Some(CustomersOutsideUK(None)),
        receiveCashPayments = Some(CashPayments(CashPaymentsCustomerNotMet(false), None))
      )

      val json = Json.obj(
        "receiveCashPayments" -> Json.obj(
          "receivePayments" -> false,
          "paymentMethods" -> Json.obj()
        ),
        "hasChanged" -> false,
        "hasAccepted" -> true
      )

      json.as[Renewal] must be(renewal)
    }
  }
}