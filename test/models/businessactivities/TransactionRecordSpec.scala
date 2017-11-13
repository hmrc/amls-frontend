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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class TransactionRecordSpec extends PlaySpec with MockitoSugar {

  "TransactionType" must {

    import jto.validation.forms.Rules._

    "pass validation" when {
      "yes is selected and a few check boxes are selected" in {

        val model = Map(
          "isRecorded" -> Seq("true"),
          "transactions[]" -> Seq("01", "02", "03"),
          "name" -> Seq("test")
        )

        KeepTransactionRecords.formRule.validate(model) must
          be(Valid(TransactionRecordYes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("test")))))

      }

      "option No is selected" in {

        val model = Map(
          "isRecorded" -> Seq("false")
        )

        KeepTransactionRecords.formRule.validate(model) must
          be(Valid(TransactionRecordNo))

      }
    }

    "fail validation" when {
      "isRecorded is not selected" in {

        val model = Map(
          "transactions[]" -> Seq("01", "02", "03"),
          "name" -> Seq("")
        )

        KeepTransactionRecords.formRule.validate(model) must
          be(Invalid(List((Path \ "isRecorded", Seq(ValidationError("error.required.ba.select.transaction.record"))))))

      }

      "isRecorded is selected and software name is empty, represented by an empty string" in {

        val model = Map(
          "isRecorded" -> Seq("true"),
          "transactions[]" -> Seq("01", "02", "03"),
          "name" -> Seq("")
        )
        KeepTransactionRecords.formRule.validate(model) must
          be(Invalid(List((Path \ "name", Seq(ValidationError("error.required.ba.software.package.name"))))))
      }

      "isRecorded is selected and software name exceeds max length" in {

        val model = Map(
          "isRecorded" -> Seq("true"),
          "transactions[]" -> Seq("01", "02", "03"),
          "name" -> Seq("a" * 41)
        )
        KeepTransactionRecords.formRule.validate(model) must
          be(Invalid(List((Path \ "name", Seq(ValidationError("error.invalid.maxlength.40"))))))
      }

      "isRecorded is selected and software name contains invalid characters" in {

        val model = Map(
          "isRecorded" -> Seq("true"),
          "transactions[]" -> Seq("01", "02", "03"),
          "name" -> Seq("abc{}abc")
        )
        KeepTransactionRecords.formRule.validate(model) must
          be(Invalid(List((Path \ "name", Seq(ValidationError("err.text.validation"))))))
      }

      "no check boxes are selected in transactions" in {

        val model = Map(
          "isRecorded" -> Seq("true"),
          "transactions[]" -> Seq(),
          "name" -> Seq("test")
        )
        KeepTransactionRecords.formRule.validate(model) must
          be(Invalid(List((Path \ "transactions", Seq(ValidationError("error.required.ba.atleast.one.transaction.record"))))))
      }

      "given an empty Map" in {

        KeepTransactionRecords.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "isRecorded") -> Seq(ValidationError("error.required.ba.select.transaction.record")))))

      }

      "given invalid enum value in transactions" in {

        val model = Map(
          "isRecorded" -> Seq("true"),
          "transactions[]" -> Seq("01, 10")
        )
        KeepTransactionRecords.formRule.validate(model) must
          be(Invalid(Seq((Path \ "transactions") -> Seq(ValidationError("error.invalid")))))

      }
    }
  }

  "form Validation" must {
    "validate form write for valid transaction record" in {

      val map = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq("03", "01"),
        "name" -> Seq("test")
      )

      val model = TransactionRecordYes(Set(DigitalSoftware("test"), Paper))
      KeepTransactionRecords.formWrites.writes(model) must be(map)
    }

    "validate form write for option No" in {

      val map = Map(
        "isRecorded" -> Seq("false")
      )
      val model = TransactionRecordNo
      KeepTransactionRecords.formWrites.writes(model) must be(map)
    }

    "validate form write for option Yes" in {

      val map = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq("02", "01")
      )

      val model = TransactionRecordYes(Set(DigitalSpreadsheet, Paper))
      KeepTransactionRecords.formWrites.writes(model) must be(map)
    }

    "form write test" in {
      val map = Map(
        "isRecorded" -> Seq("false")
      )
      val model = TransactionRecordNo

      KeepTransactionRecords.formWrites.writes(model) must be(map)
    }
  }

  "JSON validation" must {

    "successfully validate given values" in {
      val json =  Json.obj("isRecorded" -> true,
        "transactions" -> Seq("01","02"))

      Json.fromJson[KeepTransactionRecords](json) must
        be(JsSuccess(TransactionRecordYes(Set(Paper, DigitalSpreadsheet)), JsPath))
    }

    "successfully validate given values with option No" in {
      val json =  Json.obj("isRecorded" -> false)

      Json.fromJson[KeepTransactionRecords](json) must
        be(JsSuccess(TransactionRecordNo, JsPath))
    }

    "successfully validate given values with option Digital software" in {
      val json =  Json.obj("isRecorded" -> true,
        "transactions" -> Seq("03", "02"),
      "digitalSoftwareName" -> "test")

      Json.fromJson[KeepTransactionRecords](json) must
        be(JsSuccess(TransactionRecordYes(Set(DigitalSoftware("test"), DigitalSpreadsheet)), JsPath))
    }

    "fail when on path is missing" in {
      Json.fromJson[KeepTransactionRecords](Json.obj("isRecorded" -> true,
        "transaction" -> Seq("01"))) must
        be(JsError((JsPath \ "transactions") -> play.api.data.validation.ValidationError("error.path.missing")))
    }

    "fail when on invalid data" in {
      Json.fromJson[KeepTransactionRecords](Json.obj("isRecorded" -> true,"transactions" -> Seq("40"))) must
        be(JsError(((JsPath) \ "transactions") -> play.api.data.validation.ValidationError("error.invalid")))
    }

    "write valid data in using json write" in {
      Json.toJson[KeepTransactionRecords](TransactionRecordYes(Set(Paper, DigitalSoftware("test657")))) must be (Json.obj("isRecorded" -> true,
      "transactions" -> Seq("01", "03"),
        "digitalSoftwareName" -> "test657"
      ))
    }

    "write valid data in using json write with Option No" in {
      Json.toJson[KeepTransactionRecords](TransactionRecordNo) must be (Json.obj("isRecorded" -> false))
    }
  }
}


