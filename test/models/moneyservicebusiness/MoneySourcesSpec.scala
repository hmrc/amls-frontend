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

package models.moneyservicebusiness

import models.moneyservicebusiness.MoneySources.{Banks, Customers, Wholesalers}
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

    "have size method which" when {
      "called with complete model will return 3" in new Fixture {
        completeModel.size mustBe 3
      }

      "called with model containing two options will return 2" in new Fixture {
        completeModel.copy(bankMoneySource = None).size mustBe 2
      }

      "called with model containing one option will return 1" in new Fixture {
        completeModel.copy(bankMoneySource = None, wholesalerMoneySource = None).size mustBe 1
      }

      "called with empty model will return 0" in new Fixture {
        MoneySources().size mustBe 0
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

  "toFormValues" must {

    "yield the correct sequence" when {

      "model is empty" in {

        MoneySources().toFormValues.isEmpty mustBe true
      }

      val list = Seq(
        Banks,
        Wholesalers,
        Customers
      )

      list.foreach { source =>
        s"$source is present" in {

          val result = MoneySources(
            Some(BankMoneySource("A Bank")),
            Some(WholesalerMoneySource("A Wholesaler")),
            Some(true)
          ).toFormValues

          result.isEmpty mustBe false
          result(list.indexOf(source)) mustBe source
        }
      }
    }
  }

  "toMessages" must {

    "yield the correct messages" in {

      val result = MoneySources(
        Some(BankMoneySource("A Bank")),
        Some(WholesalerMoneySource("A Wholesaler")),
        Some(true)
      ).toMessages

      result.head mustBe messages("msb.which_currencies.source.banks")
      result(1) mustBe messages("msb.which_currencies.source.wholesalers")
      result(2) mustBe messages("msb.which_currencies.source.customers")

      MoneySources().toMessages.isEmpty mustBe true
    }
  }
}
