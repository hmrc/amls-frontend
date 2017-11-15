/*
 * Copyright 2017 HM Revenue & Customs
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

package models.businessactivities

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, Json}

class TransactionTypeSpec extends PlaySpec with MustMatchers {

  "TransactionType" must {
    "pass validation" when {
      "yes is selected and a few check boxes are selected" in {
        val model = Map(
          "types[]" -> Seq("01", "02", "03"),
          "name" -> Seq("test")
        )

        TransactionTypes.formRule.validate(model) must
          be(Valid(TransactionTypes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("test")))))
      }
    }

    "fail validation" when {
      "software is selected but software name is empty, represented by an empty string" in {
        val model = Map(
          "types[]" -> Seq("01", "02", "03"),
          "name" -> Seq("")
        )

        TransactionTypes.formRule.validate(model) must
          be(Invalid(List((Path \ "name", Seq(ValidationError("error.required.ba.software.package.name"))))))
      }

      "software name exceeds max length" in {
        val model = Map(
          "types[]" -> Seq("01", "02", "03"),
          "name" -> Seq("a" * 41)
        )

        TransactionTypes.formRule.validate(model) must
          be(Invalid(List((Path \ "name", Seq(ValidationError("error.invalid.maxlength.40"))))))
      }

      "software name contains invalid characters" in {
        val model = Map(
          "types[]" -> Seq("01", "02", "03"),
          "name" -> Seq("abc{}abc")
        )

        TransactionTypes.formRule.validate(model) must
          be(Invalid(List((Path \ "name", Seq(ValidationError("err.text.validation"))))))
      }

      "no check boxes are selected in transactions" in {
        val model = Map(
          "types[]" -> Seq()
        )

        TransactionTypes.formRule.validate(model) must
          be(Invalid(List((Path \ "types", Seq(ValidationError("error.required.ba.atleast.one.transaction.record"))))))
      }

      "given an empty Map" in {
        TransactionTypes.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "types") -> Seq(ValidationError("error.required.ba.atleast.one.transaction.record")))))
      }

      "given invalid enum value in transactions" in {
        val model = Map(
          "types[]" -> Seq("01, 10")
        )

        TransactionTypes.formRule.validate(model) must
          be(Invalid(Seq((Path \ "types") -> Seq(ValidationError("error.invalid")))))
      }
    }

    "write values to the form " in {
      val model = TransactionTypes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("test")))
      val output = TransactionTypes.formWriter.writes(model)

      output mustBe Map(
        "types[]" -> Seq("01", "02", "03"),
        "name" -> Seq("test")
      )
    }

    "write values to the form without the name" in {
      val model = TransactionTypes(Set(Paper))

      TransactionTypes.formWriter.writes(model) mustBe Map(
        "types[]" -> Seq("01")
      )
    }

    "write values to JSON" in {
      val model = TransactionTypes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("test")))

      Json.toJson(model) mustBe Json.obj(
        "types" -> Seq("01", "02", "03"),
        "name" -> "test"
      )
    }

    "read values from JSON" in {
      val json = Json.obj(
        "types" -> Seq("01", "02", "03"),
        "software" -> "example software"
      )

      json.asOpt[TransactionTypes] mustBe Some(TransactionTypes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("example software"))))
    }

    "read values from JSON without software" in {
      val json = Json.obj(
        "types" -> Seq("01", "02")
      )

      json.asOpt[TransactionTypes] mustBe Some(TransactionTypes(Set(Paper, DigitalSpreadsheet)))
    }

    "fail when the name not supplied in the Json" in {
      val json = Json.obj(
        "types" -> Seq("01", "02", "03")
      )

      Json.fromJson[TransactionTypes](json) mustBe JsError(JsPath \ "software" -> play.api.data.validation.ValidationError("error.missing"))
    }

    "fail when an invalid value was given" in {
      val json = Json.obj(
        "types" -> Seq("01", "10")
      )

      Json.fromJson[TransactionTypes](json) mustBe JsError(JsPath \ "types" -> play.api.data.validation.ValidationError("error.invalid"))
    }

    "fail when no values are given" in {
      val json = Json.obj()

      Json.fromJson[TransactionTypes](json) mustBe JsError(JsPath \ "types" -> play.api.data.validation.ValidationError("error.missing"))
    }
  }
}
