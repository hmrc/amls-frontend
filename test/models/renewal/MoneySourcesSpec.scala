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

import jto.validation.{Invalid, Path, Valid, ValidationError}
import models.CharacterSets
import play.api.libs.json.{JsPath, JsSuccess, Json}
import utils.AmlsSpec

class MoneySourcesSpec extends AmlsSpec {

  trait Fixture {
    val completeModel = MoneySources(
      Some(BankMoneySource("Bank names")),
      Some(WholesalerMoneySource("Wholesaler names")),
      Some(true)
    )

    def buildString(length: Int, acc: String = ""): String = {
      length match {
        case 0 => ""
        case 1 => "X"
        case _ => "X" ++ buildString(length - 1)
      }
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
        val data = MoneySources(
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
        val data = MoneySources(
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
    "successfully validate" when {
      "bank money source selected" in new Fixture {
        val form =  Map(
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq("Bank names")
        )

        val expected = Valid(completeModel.copy(wholesalerMoneySource = None, customerMoneySource = None))

        MoneySources.formRule.validate(form) must be(expected)
      }

      "wholesaler money source selected" in new Fixture {
        val form =  Map(
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq("Wholesaler names")
        )

        val expected = Valid(completeModel.copy(bankMoneySource = None, customerMoneySource = None))

        MoneySources.formRule.validate(form) must be(expected)
      }

      "customer money source selected" in new Fixture{
        val form =  Map(
          "customerMoneySource" -> Seq("Yes")
        )

        val expected = Valid(completeModel.copy(bankMoneySource = None, wholesalerMoneySource = None))

        MoneySources.formRule.validate(form) must be(expected)
      }

      "all options selected" in new Fixture {
        val form =  Map(
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq("Bank names"),
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq("Wholesaler names"),
          "customerMoneySource" -> Seq("Yes")
        )

        val expected = Valid(completeModel)

        MoneySources.formRule.validate(form) must be(expected)
      }

      "bankNames and wholesalerNames containing standard UK alpha characters" in new Fixture with CharacterSets {
        val alpha = (alphaLower.take(42) ++ alphaUpper.take(42)).mkString("")

        val form =  Map(
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq(alpha),
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq(alpha)
        )
        val expected = Valid(completeModel.copy(Some(BankMoneySource(alpha)), Some(WholesalerMoneySource(alpha)), None))

        MoneySources.formRule.validate(form) must be(expected)
      }

      "bankNames and wholesalerNames containing accented characters" in new Fixture with CharacterSets {
        val accentedAlpha = (extendedAlphaLower.take(42) ++ extendedAlphaUpper.take(42)).mkString("")

        val form =  Map(
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq(accentedAlpha),
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq(accentedAlpha)
        )
        val expected = Valid(completeModel.copy(Some(BankMoneySource(accentedAlpha)), Some(WholesalerMoneySource(accentedAlpha)), None))

        MoneySources.formRule.validate(form) must be(expected)
      }
    }

    "successfully write to form" when {
      "given one source" in new Fixture {
        val sources = completeModel.copy(wholesalerMoneySource = None, customerMoneySource = None)
        val expected = Map(
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq("Bank names")
        )

        MoneySources.formWrite.writes(sources) mustBe(expected)
      }

      "given all sources" in new Fixture {
        val sources = completeModel
        val expected = Map(
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq("Bank names"),
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq("Wholesaler names"),
          "customerMoneySource" -> Seq("Yes")
        )

        MoneySources.formWrite.writes(sources) mustBe(expected)
      }

      "given customer only source" in new Fixture {
        val sources = completeModel.copy(wholesalerMoneySource = None, bankMoneySource = None)
        val expected = Map(
          "customerMoneySource" -> Seq("Yes")
        )

        MoneySources.formWrite.writes(sources) mustBe(expected)
      }
    }

    "fail validation" when {
      "wholesalerMoneySource selected and no names provided" in new Fixture {
        val form =  Map(
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq("")
        )
        val expected = Invalid(Seq((Path \ "wholesalerNames") -> Seq(ValidationError("error.invalid.renewal.msb.wc.wholesalerNames"))))
        MoneySources.formRule.validate(form) must be(expected)
      }

      "submitting empty form" in new Fixture {
        val form: Map[String, Seq[String]] = Map.empty

        val expected = Invalid(Seq((Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.renewal.msb.wc.moneySources"))))
        MoneySources.formRule.validate(form) must be(expected)
      }

      "bankMoneySource selected and no names provided" in new Fixture {
        val form =  Map(
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq("")
        )
        val expected = Invalid(Seq((Path \ "bankNames") -> Seq(ValidationError("error.invalid.renewal.msb.wc.bankNames"))))
        MoneySources.formRule.validate(form) must be(expected)
      }

      "bankNames and wholesalerNames longer than 140 characters" in new Fixture {
        val form =  Map(
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq(buildString(141)),
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq(buildString(141))
        )
        val expected = Invalid(Seq(
          (Path \ "bankNames") -> Seq(ValidationError("error.invalid.maxlength.140.bankNames")),
          (Path \ "wholesalerNames") -> Seq(ValidationError("error.invalid.maxlength.140.wholesalerNames"))))

        MoneySources.formRule.validate(form) must be(expected)
      }

      "bankNames and wholesalerNames containing not allowed characters" in new Fixture with CharacterSets {
        val form =  Map(
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq(symbols5.mkString("")),
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq(symbols5.mkString(""))
        )
        val expected = Invalid(Seq(
          (Path \ "bankNames") -> Seq(ValidationError("error.invalid.characters.renewal.msb.wc.bankNames")),
          (Path \ "wholesalerNames") -> Seq(ValidationError("error.invalid.characters.renewal.msb.wc.wholesalerNames"))))

        MoneySources.formRule.validate(form) must be(expected)
      }

      "bankNames and wholesalerNames containing white spaces" in new Fixture with CharacterSets {
        val form =  Map(
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq("    "),
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq("    ")
        )
        val expected = Invalid(Seq(
          (Path \ "bankNames") -> Seq(ValidationError("error.invalid.renewal.msb.wc.bankNames")),
          (Path \ "wholesalerNames") -> Seq(ValidationError("error.invalid.renewal.msb.wc.wholesalerNames"))))

        MoneySources.formRule.validate(form) must be(expected)
      }
    }
  }

  "Json reads and writes" must {
    "Serialize bank money sources as expected" in new Fixture {
      Json.toJson(completeModel.copy(wholesalerMoneySource = None, customerMoneySource = None)) must be(Json.obj("bankMoneySource" -> "Yes",
        "bankNames" -> "Bank names"))
    }

    "Serialize wholesaler money sources as expected" in new Fixture {
      Json.toJson(completeModel.copy(bankMoneySource = None, customerMoneySource = None)) must be(Json.obj("wholesalerMoneySource" -> "Yes",
        "wholesalerNames" -> "Wholesaler names"))
    }

    "Serialize customer money sources as expected" in new Fixture {
      Json.toJson(completeModel.copy(wholesalerMoneySource = None, bankMoneySource = None)) must be(Json.obj("customerMoneySource" -> "Yes"))
    }

    "Serialize all sources as expected" in new Fixture {
      Json.toJson(completeModel) must be(
        Json.obj("bankMoneySource" -> "Yes",
          "bankNames" -> "Bank names")
          ++ Json.obj("wholesalerMoneySource" -> "Yes",
          "wholesalerNames" -> "Wholesaler names")
          ++ Json.obj("customerMoneySource" -> "Yes"))
    }

    "Deserialize bank money sources as expected" in new Fixture {
      val json = Json.obj("bankMoneySource" -> "Yes", "bankNames" -> "Bank names")
      val expected = JsSuccess(completeModel.copy(wholesalerMoneySource = None, customerMoneySource = None), JsPath)
      Json.fromJson[MoneySources](json) must be(expected)
    }

    "Deserialize wholesaler money sources as expected" in new Fixture {
      val json = Json.obj("wholesalerMoneySource" -> "Yes", "wholesalerNames" -> "Wholesaler names")
      val expected = JsSuccess(completeModel.copy(bankMoneySource = None, customerMoneySource = None), JsPath)
      Json.fromJson[MoneySources](json) must be(expected)
    }

    "Deserialize customer money sources as expected" in new Fixture {
      val json = Json.obj("customerMoneySource" -> "Yes")
      val expected = JsSuccess(completeModel.copy(wholesalerMoneySource = None, bankMoneySource = None), JsPath)
      Json.fromJson[MoneySources](json) must be(expected)
    }

    "Deserialize all money sources as expected" in new Fixture {
      val json = Json.obj("bankMoneySource" -> "Yes", "bankNames" -> "Bank names",
        "wholesalerMoneySource" -> "Yes", "wholesalerNames" -> "Wholesaler names",
        "customerMoneySource" -> "Yes")
      val expected = JsSuccess(completeModel, JsPath)
      Json.fromJson[MoneySources](json) must be(expected)
    }
  }
}
