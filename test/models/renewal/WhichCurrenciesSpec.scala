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

import models.{CharacterSets, moneyservicebusiness => msb}
import play.api.libs.json._
import utils.AmlsSpec

class WhichCurrenciesSpec extends AmlsSpec with CharacterSets {

  "Which Currencies" when {
    "data is complete" should {

      val fullModel = WhichCurrencies(
        Seq("USD", "CHF", "EUR"),
        None,
        Some(MoneySources(None, None, None))
      )

      "Round trip through Json correctly" in {
        val js = Json.toJson(fullModel)
        js.as[WhichCurrencies] must be(fullModel)
      }
    }

    "Json read and writes" must {
      "Serialise WhichCurrencies as expected" in {

        val input = WhichCurrencies(
          Seq("USD", "CHF", "EUR"),
          Some(UsesForeignCurrenciesYes),
          Some(MoneySources(None, None, None))
        )

        val expectedJson = Json.obj(
          "currencies"            -> Seq("USD", "CHF", "EUR"),
          "usesForeignCurrencies" -> UsesForeignCurrenciesYes.asInstanceOf[UsesForeignCurrencies],
          "moneySources"          -> Json.obj()
        )

        Json.toJson(input) must be(expectedJson)
      }

      "Deserialize WhichCurrencies as expected" in {

        val inputJson = Json.obj(
          "currencies"            -> Seq("USD", "CHF", "EUR"),
          "usesForeignCurrencies" -> UsesForeignCurrenciesYes.asInstanceOf[UsesForeignCurrencies],
          "moneySources"          -> Json.obj()
        )

        val expected = WhichCurrencies(
          Seq("USD", "CHF", "EUR"),
          Some(UsesForeignCurrenciesYes),
          Some(MoneySources(None, None, None))
        )

        Json.fromJson[WhichCurrencies](inputJson) must be(JsSuccess(expected, JsPath))
      }

      "fail when missing all data" in {
        Json.fromJson[WhichCurrencies](Json.obj()) must be
        JsError((JsPath \ "currencies")              -> play.api.libs.json.JsonValidationError("error.path.missing"))
        JsError((JsPath \ "customerMoneySource")     -> play.api.libs.json.JsonValidationError("error.path.missing"))
        JsError((JsPath \ "currencies")              -> play.api.libs.json.JsonValidationError("error.path.missing"))
      }
    }

    "convert function" should {
      "convert renewal which currencies to msb which currencies for UsesForeignCurrenciesNo" in {
        val msbWc     = msb.WhichCurrencies(Seq("USD"), Some(msb.UsesForeignCurrenciesNo), Some(msb.MoneySources()))
        val renewalWc = WhichCurrencies(Seq("USD"), Some(UsesForeignCurrenciesNo), Some(MoneySources()))

        WhichCurrencies.convert(renewalWc) mustBe msbWc
      }

      "convert renewal which currencies to msb which currencies for UsesForeignCurrenciesYes and BankMoneySources" in {
        val msbWc     = msb.WhichCurrencies(
          Seq("USD"),
          Some(msb.UsesForeignCurrenciesYes),
          Some(msb.MoneySources(Some(msb.BankMoneySource("Bank names"))))
        )
        val renewalWc = WhichCurrencies(
          Seq("USD"),
          Some(UsesForeignCurrenciesYes),
          Some(MoneySources(Some(BankMoneySource("Bank names"))))
        )

        WhichCurrencies.convert(renewalWc) mustBe msbWc
      }

      "convert renewal which currencies to msb which currencies for UsesForeignCurrenciesYes and WholesalerMoneySources" in {
        val msbWc     = msb.WhichCurrencies(
          Seq("USD"),
          Some(msb.UsesForeignCurrenciesYes),
          Some(msb.MoneySources(wholesalerMoneySource = Some(msb.WholesalerMoneySource("Wholesaler names"))))
        )
        val renewalWc = WhichCurrencies(
          Seq("USD"),
          Some(UsesForeignCurrenciesYes),
          Some(MoneySources(None, Some(WholesalerMoneySource("Wholesaler names")), None))
        )

        WhichCurrencies.convert(renewalWc) mustBe msbWc
      }

      "convert renewal which currencies to msb which currencies for UsesForeignCurrenciesYes and CustomerMoneySources" in {
        val msbWc     = msb.WhichCurrencies(
          Seq("USD"),
          Some(msb.UsesForeignCurrenciesYes),
          Some(msb.MoneySources(customerMoneySource = Some(true)))
        )
        val renewalWc =
          WhichCurrencies(Seq("USD"), Some(UsesForeignCurrenciesYes), Some(MoneySources(None, None, Some(true))))

        WhichCurrencies.convert(renewalWc) mustBe msbWc
      }

      "convert renewal which currencies to msb which currencies for UsesForeignCurrenciesYes and all money sources" in {
        val msbWc     = msb.WhichCurrencies(
          Seq("USD"),
          Some(msb.UsesForeignCurrenciesYes),
          Some(
            msb.MoneySources(
              Some(msb.BankMoneySource("Bank names")),
              Some(msb.WholesalerMoneySource("Wholesaler names")),
              Some(true)
            )
          )
        )
        val renewalWc = WhichCurrencies(
          Seq("USD"),
          Some(UsesForeignCurrenciesYes),
          Some(
            MoneySources(
              Some(BankMoneySource("Bank names")),
              Some(WholesalerMoneySource("Wholesaler names")),
              Some(true)
            )
          )
        )

        WhichCurrencies.convert(renewalWc) mustBe msbWc
      }
    }
  }
}
