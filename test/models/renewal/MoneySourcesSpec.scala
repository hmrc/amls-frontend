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

import play.api.libs.json.{JsPath, JsSuccess, Json}
import utils.AmlsSpec

class MoneySourcesSpec extends AmlsSpec {

  trait Fixture {
    val completeModel = MoneySources(
      Some(BankMoneySource("Bank names")),
      Some(WholesalerMoneySource("Wholesaler names")),
      Some(true)
    )

    def buildString(length: Int, acc: String = ""): String =
      length match {
        case 0 => ""
        case 1 => "X"
        case _ => "X" ++ buildString(length - 1)
      }
  }

  "MoneySources" must {
    "count the amount of attribute a use has selected" when {
      "given all 3 attributes" in {
        val data = MoneySources(
          Some(BankMoneySource("Bank names")),
          Some(WholesalerMoneySource("Wholesaler names")),
          Some(true)
        )
        data.size mustBe 3
      }

      "show 2 when user has selected any 2 options" in {
        val data  = MoneySources(
          None,
          Some(WholesalerMoneySource("Wholesaler names")),
          Some(true)
        )
        val data2 = MoneySources(
          Some(BankMoneySource("Bank names")),
          None,
          Some(true)
        )
        val data3 = MoneySources(
          Some(BankMoneySource("Bank names")),
          Some(WholesalerMoneySource("Wholesaler names")),
          None
        )
        data.size mustBe 2
        data2.size mustBe 2
        data3.size mustBe 2
      }

      "show 1 when user has selected any singular option" in {
        val data  = MoneySources(
          None,
          None,
          Some(true)
        )
        val data2 = MoneySources(
          Some(BankMoneySource("Bank names")),
          None,
          None
        )
        val data3 = MoneySources(
          None,
          Some(WholesalerMoneySource("Wholesaler names")),
          None
        )
        data.size mustBe 1
        data2.size mustBe 1
        data3.size mustBe 1
      }
    }
  }

  "Json reads and writes" must {
    "Serialize bank money sources as expected" in new Fixture {
      Json.toJson(completeModel.copy(wholesalerMoneySource = None, customerMoneySource = None)) must be(
        Json.obj("bankMoneySource" -> "Yes", "bankNames" -> "Bank names")
      )
    }

    "Serialize wholesaler money sources as expected" in new Fixture {
      Json.toJson(completeModel.copy(bankMoneySource = None, customerMoneySource = None)) must be(
        Json.obj("wholesalerMoneySource" -> "Yes", "wholesalerNames" -> "Wholesaler names")
      )
    }

    "Serialize customer money sources as expected" in new Fixture {
      Json.toJson(completeModel.copy(wholesalerMoneySource = None, bankMoneySource = None)) must be(
        Json.obj("customerMoneySource" -> "Yes")
      )
    }

    "Serialize all sources as expected" in new Fixture {
      Json.toJson(completeModel) must be(
        Json.obj("bankMoneySource" -> "Yes", "bankNames" -> "Bank names")
          ++ Json.obj("wholesalerMoneySource" -> "Yes", "wholesalerNames" -> "Wholesaler names")
          ++ Json.obj("customerMoneySource" -> "Yes")
      )
    }

    "Deserialize bank money sources as expected" in new Fixture {
      val json     = Json.obj("bankMoneySource" -> "Yes", "bankNames" -> "Bank names")
      val expected = JsSuccess(completeModel.copy(wholesalerMoneySource = None, customerMoneySource = None), JsPath)
      Json.fromJson[MoneySources](json) must be(expected)
    }

    "Deserialize wholesaler money sources as expected" in new Fixture {
      val json     = Json.obj("wholesalerMoneySource" -> "Yes", "wholesalerNames" -> "Wholesaler names")
      val expected = JsSuccess(completeModel.copy(bankMoneySource = None, customerMoneySource = None), JsPath)
      Json.fromJson[MoneySources](json) must be(expected)
    }

    "Deserialize customer money sources as expected" in new Fixture {
      val json     = Json.obj("customerMoneySource" -> "Yes")
      val expected = JsSuccess(completeModel.copy(wholesalerMoneySource = None, bankMoneySource = None), JsPath)
      Json.fromJson[MoneySources](json) must be(expected)
    }

    "Deserialize all money sources as expected" in new Fixture {
      val json     = Json.obj(
        "bankMoneySource"       -> "Yes",
        "bankNames"             -> "Bank names",
        "wholesalerMoneySource" -> "Yes",
        "wholesalerNames"       -> "Wholesaler names",
        "customerMoneySource"   -> "Yes"
      )
      val expected = JsSuccess(completeModel, JsPath)
      Json.fromJson[MoneySources](json) must be(expected)
    }
  }
}
