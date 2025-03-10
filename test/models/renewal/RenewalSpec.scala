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

package models.renewal

import models.Country
import play.api.libs.json.{JsSuccess, Json}
import utils.AmlsSpec

class RenewalSpec extends AmlsSpec {

  val completeRenewal = Renewal(
    Some(InvolvedInOtherYes("test")),
    Some(BusinessTurnover.First),
    Some(AMLSTurnover.First),
    Some(AMPTurnover.First),
    Some(CustomersOutsideIsUK(false)),
    Some(CustomersOutsideUK(Option(Nil))),
    Some(PercentageOfCashPaymentOver15000.First),
    Some(
      CashPayments(
        CashPaymentsCustomerNotMet(true),
        Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other"))))
      )
    ),
    Some(TotalThroughput("01")),
    Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
    Some(TransactionsInLast12Months("1500")),
    Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
    Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
    Some(CETransactionsInLast12Months("123")),
    hasChanged = true,
    sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true))
  )

  "The Renewal model" must {
    "succesfully validate if model is complete" when {
      "json is complete" in {
        val completeRenewal = Renewal(
          customersOutsideIsUK = Some(CustomersOutsideIsUK(true)),
          customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB")))))
        )

        val json = Json.obj(
          "customersOutsideIsUK" -> Json.obj(
            "isOutside" -> true
          ),
          "customersOutsideUK"   -> Json.obj(
            "countries" -> Seq("GB")
          ),
          "hasChanged"           -> false,
          "hasAccepted"          -> true
        )

        json.as[Renewal] must be(completeRenewal)
      }
    }

    "succesfully validate json" when {
      "CustomersOutsideIsUK is false" in {
        val renewal = Renewal(
          customersOutsideIsUK = Some(CustomersOutsideIsUK(false)),
          customersOutsideUK = Some(CustomersOutsideUK(None))
        )

        val json = Json.obj(
          "customersOutsideUK" -> Json.obj(
            "isOutside" -> false
          ),
          "hasChanged"         -> false,
          "hasAccepted"        -> true
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
        Some(
          CashPayments(
            CashPaymentsCustomerNotMet(true),
            Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other"))))
          )
        ),
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

      Json.fromJson[Renewal](Json.toJson(completeRenewal)) mustEqual JsSuccess(completeRenewal)
    }

    "have validation rules that behave correctly" when {

      ".standardRule is called" must {

        "return true" when {

          "minimum standards are met for submission" in {

            completeRenewal.standardRule mustBe true
          }
        }

        "return false" when {

          "involvedInOtherActivities is not defined" in {

            completeRenewal.copy(involvedInOtherActivities = None, businessTurnover = None).standardRule mustBe false
          }

          "turnover is not defined" in {

            completeRenewal.copy(turnover = None).standardRule mustBe false
          }

          "business is involved in other activities but businessTurnover is empty" in {

            completeRenewal.copy(businessTurnover = None).standardRule mustBe false
          }

          "business is not involved in other activities but businessTurnover is non-empty" in {

            completeRenewal.copy(involvedInOtherActivities = None).standardRule mustBe false
          }

          "has accepted is false" in {

            completeRenewal.copy(hasAccepted = false).standardRule mustBe false
          }
        }
      }

      ".hvdRule is called" must {

        "return true" when {

          "all HVD data is populated to correct standard" in {

            completeRenewal.hvdRule mustBe true

            completeRenewal
              .copy(
                receiveCashPayments = Some(CashPayments(CashPaymentsCustomerNotMet(false), None)),
                customersOutsideIsUK = Some(CustomersOutsideIsUK(false))
              )
              .hvdRule mustBe true
          }
        }

        "return false" when {

          "percentageOfCashPaymentOver15000 is empty" in {

            completeRenewal.copy(percentageOfCashPaymentOver15000 = None).hvdRule mustBe false
          }

          "receiveCashPayments is true with no method of receipt" in {

            completeRenewal
              .copy(
                receiveCashPayments = Some(CashPayments(CashPaymentsCustomerNotMet(true), None))
              )
              .hvdRule mustBe false
          }

          "receiveCashPayments is false with a method of receipt" in {

            completeRenewal
              .copy(
                receiveCashPayments = Some(
                  CashPayments(
                    CashPaymentsCustomerNotMet(false),
                    Some(HowCashPaymentsReceived(PaymentMethods(true, true, None)))
                  )
                )
              )
              .hvdRule mustBe false
          }

          "receiveCashPayments is empty" in {

            completeRenewal.copy(receiveCashPayments = None).hvdRule mustBe false
          }

          "customersOutsideIsUK is true with no list of countries" in {

            completeRenewal
              .copy(
                customersOutsideIsUK = Some(CustomersOutsideIsUK(true)),
                customersOutsideUK = None
              )
              .hvdRule mustBe false
          }

          "customersOutsideIsUK is empty" in {

            completeRenewal.copy(customersOutsideIsUK = None).hvdRule mustBe false
          }
        }
      }

      ".currencyExchangeRule is called" must {

        "return true" when {

          "whichCurrencies and ceTransactionsInLast12Months are defined" in {

            completeRenewal.currencyExchangeRule mustBe true
          }
        }

        "return false" when {

          "whichCurrencies is empty" in {

            completeRenewal.copy(whichCurrencies = None).currencyExchangeRule mustBe false
          }

          "ceTransactionsInLast12Months is empty" in {

            completeRenewal.copy(ceTransactionsInLast12Months = None).currencyExchangeRule mustBe false
          }
        }
      }

      ".moneyTransmitterRule is called" must {

        "return true" when {

          "all money transmission questions are populated for Yes path" in {

            completeRenewal.moneyTransmitterRule mustBe true
          }

          "all money transmission questions are populated for No path" in {

            completeRenewal
              .copy(
                sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false)),
                mostTransactions = None
              )
              .moneyTransmitterRule mustBe true

            completeRenewal
              .copy(
                sendMoneyToOtherCountry = None,
                mostTransactions = None
              )
              .moneyTransmitterRule mustBe true
          }
        }

        "return false" when {

          "sendMoneyToOtherCountry is empty with other questions answered" in {

            completeRenewal.copy(sendMoneyToOtherCountry = None).moneyTransmitterRule mustBe false
          }

          "transactionsInLast12Months is empty" in {

            completeRenewal.copy(transactionsInLast12Months = None).moneyTransmitterRule mustBe false
          }

          "mostTransactions is empty with other questions answered" in {

            completeRenewal.copy(mostTransactions = None).moneyTransmitterRule mustBe false
          }

          "sendTheLargestAmountsOfMoney is empty with other questions answered" in {

            completeRenewal.copy(sendTheLargestAmountsOfMoney = None).moneyTransmitterRule mustBe false
          }
        }
      }
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
          "paymentMethods"  -> Json.obj()
        ),
        "hasChanged"          -> false,
        "hasAccepted"         -> true
      )

      json.as[Renewal] must be(renewal)
    }
  }
}
