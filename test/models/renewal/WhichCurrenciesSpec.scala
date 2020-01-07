/*
 * Copyright 2020 HM Revenue & Customs
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

import jto.validation._
import models.CharacterSets
import play.api.libs.json._
import utils.AmlsSpec
import models.{moneyservicebusiness => msb}

import scala.collection.mutable.ArrayBuffer

class WhichCurrenciesSpec extends AmlsSpec with CharacterSets {

  "Which Currencies" when {
    "data is complete" should {

      val fullModel = WhichCurrencies(
        Seq("USD", "CHF", "EUR"), None, Some(MoneySources(None, None, None))
      )

      val fullFormData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR")
      )

      "Write correctly to a form" in {
        WhichCurrencies.formWrite.writes(fullModel) must be(fullFormData)
      }

      "Read correctly from a form" in {
        WhichCurrencies.formRule.validate(fullFormData) must be(Valid(fullModel.copy(moneySources = None)))
      }


      "Round trip through Json correctly" in {
        val js = Json.toJson(fullModel)
        js.as[WhichCurrencies] must be(fullModel)
      }

      "Fail validation" when {
        "currencies are sent as blank strings" in {
          val formData = Map(
            "currencies[0]" -> Seq(""),
            "currencies[1]" -> Seq(""),
            "currencies[2]" -> Seq("")
          )

          WhichCurrencies.formRule.validate(formData) equals
            Invalid(Seq(jto.validation.Path \ "currencies" -> ArrayBuffer(ValidationError("error.invalid.msb.wc.currencies"))))
        }

        "entered currency code is not valid" in {
          val formData = Map(
            "currencies[0]" -> Seq("ZZZ"),
            "currencies[1]" -> Seq(""),
            "currencies[2]" -> Seq("")
          )

          WhichCurrencies.formRule.validate(formData) equals
            Invalid(Seq(jto.validation.Path \ "currencies[0]" -> ArrayBuffer(ValidationError("error.invalid.msb.wc.currencies"))))
        }

        "data is missing" in {
          val formData: Map[String, Seq[String]] = Map.empty

          WhichCurrencies.formRule.validate(formData) equals
            Invalid(Seq(jto.validation.Path \ "currencies" -> ArrayBuffer(ValidationError("error.invalid.msb.wc.currencies"))))
        }
      }
    }

    "Json read and writes" must {
      "Serialise WhichCurrencies as expected" in {

        val input = WhichCurrencies(Seq("USD", "CHF", "EUR"), Some(UsesForeignCurrenciesYes), Some(MoneySources(None, None, None)))

        val expectedJson = Json.obj("currencies" -> Seq("USD", "CHF", "EUR"), "usesForeignCurrencies" -> UsesForeignCurrenciesYes, "moneySources" -> Json.obj())

        Json.toJson(input) must be(expectedJson)
      }


      "Deserialize WhichCurrencies as expected" in {

        val inputJson = Json.obj("currencies" -> Seq("USD", "CHF", "EUR"), "usesForeignCurrencies" -> UsesForeignCurrenciesYes, "moneySources" -> Json.obj())

        val expected = WhichCurrencies(Seq("USD", "CHF", "EUR"), Some(UsesForeignCurrenciesYes), Some(MoneySources(None, None, None)))

        Json.fromJson[WhichCurrencies](inputJson) must be (JsSuccess(expected, JsPath))
      }

      "fail when missing all data" in {
        Json.fromJson[WhichCurrencies](Json.obj()) must be
        JsError((JsPath \ "currencies") -> play.api.data.validation.ValidationError("error.path.missing"))
        JsError((JsPath \ "customerMoneySource") -> play.api.data.validation.ValidationError("error.path.missing"))
        JsError((JsPath \ "currencies") -> play.api.data.validation.ValidationError("error.path.missing"))
      }
    }

    "convert function" should {
      "convert renewal which currencies to msb which currencies for UsesForeignCurrenciesNo" in {
        val msbWc = msb.WhichCurrencies(Seq("USD"), Some(msb.UsesForeignCurrenciesNo), Some(msb.MoneySources()))
        val renewalWc = WhichCurrencies(Seq("USD"), Some(UsesForeignCurrenciesNo), Some(MoneySources()))

        WhichCurrencies.convert(renewalWc) mustBe msbWc
      }

      "convert renewal which currencies to msb which currencies for UsesForeignCurrenciesYes and BankMoneySources" in {
        val msbWc = msb.WhichCurrencies(Seq("USD"), Some(msb.UsesForeignCurrenciesYes), Some(msb.MoneySources(Some(msb.BankMoneySource("Bank names")))))
        val renewalWc = WhichCurrencies(Seq("USD"), Some(UsesForeignCurrenciesYes), Some(MoneySources(Some(BankMoneySource("Bank names")))))

        WhichCurrencies.convert(renewalWc) mustBe msbWc
      }

      "convert renewal which currencies to msb which currencies for UsesForeignCurrenciesYes and WholesalerMoneySources" in {
        val msbWc = msb.WhichCurrencies(Seq("USD"), Some(msb.UsesForeignCurrenciesYes), Some(msb.MoneySources(wholesalerMoneySource = Some(msb.WholesalerMoneySource("Wholesaler names")))))
        val renewalWc = WhichCurrencies(Seq("USD"), Some(UsesForeignCurrenciesYes), Some(MoneySources(None, Some(WholesalerMoneySource("Wholesaler names")), None)))

        WhichCurrencies.convert(renewalWc) mustBe msbWc
      }

      "convert renewal which currencies to msb which currencies for UsesForeignCurrenciesYes and CustomerMoneySources" in {
        val msbWc = msb.WhichCurrencies(Seq("USD"), Some(msb.UsesForeignCurrenciesYes), Some(msb.MoneySources(customerMoneySource = Some(true))))
        val renewalWc = WhichCurrencies(Seq("USD"), Some(UsesForeignCurrenciesYes), Some(MoneySources(None, None, Some(true))))

        WhichCurrencies.convert(renewalWc) mustBe msbWc
      }

      "convert renewal which currencies to msb which currencies for UsesForeignCurrenciesYes and all money sources" in {
        val msbWc = msb.WhichCurrencies(Seq("USD"), Some(msb.UsesForeignCurrenciesYes), Some(msb.MoneySources(Some(msb.BankMoneySource("Bank names")), Some(msb.WholesalerMoneySource("Wholesaler names")),Some(true))))
        val renewalWc = WhichCurrencies(Seq("USD"), Some(UsesForeignCurrenciesYes), Some(MoneySources(Some(BankMoneySource("Bank names")), Some(WholesalerMoneySource("Wholesaler names")), Some(true))))

        WhichCurrencies.convert(renewalWc) mustBe msbWc
      }
    }
  }
}
