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

package models.responsiblepeople

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class UKPassportSpec extends PlaySpec {

  "Uk passport number" must {
    "pass validation" when {
      "given the correct number of numbers" in {
        UKPassport.ukPassportType.validate("123456789") mustBe Valid("123456789")
      }
    }

    "fail validation" when {
      "the passport number has too many characters" in {
        UKPassport.ukPassportType.validate("a" * 10) mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.uk.passport")))
        )
      }
      "the passport number has too few characters" in {
        UKPassport.ukPassportType.validate("a" * 8) mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.uk.passport")))
        )
      }
      "the passport number includes invalid characters (letters, punctuation etc)" in {
        UKPassport.ukPassportType.validate("123abc7{}") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.invalid.uk.passport")))
        )
      }
      "the passport number is an empty string" in {
        UKPassport.ukPassportType.validate("") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.uk.passport")))
        )
      }
      "the passport number is given a sequence of whitespace" in {
        UKPassport.ukPassportType.validate("    ") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.uk.passport")))
        )
      }
    }
  }

  "UKPassport" must {

    "pass validation for uk passport number" in {
      val urlFormEncoded = Map(
        "ukPassport" -> Seq("true"),
        "ukPassportNumber" -> Seq("000000000")
      )
      UKPassport.formRule.validate(urlFormEncoded) must be(Valid(UKPassportYes("000000000")))
    }

    "pass validation for no uk passport" in {
      val urlFormEncoded = Map("ukPassport" -> Seq("false"))
      UKPassport.formRule.validate(urlFormEncoded) must be(Valid(UKPassportNo))
    }

    "fail to validate given an invalid passport number" in {

      val urlFormEncoded = Map(
        "ukPassport" -> Seq("true"),
        "ukPassportNumber" -> Seq("10")
      )

      UKPassport.formRule.validate(urlFormEncoded) must
        be(Invalid(Seq(
          (Path \ "ukPassportNumber") -> Seq(ValidationError("error.required.uk.passport"))
        )))
    }

    "fail to validate given an invalid ukPassport value" in {

      val urlFormEncoded = Map("ukPassport" -> Seq("10"))

      UKPassport.formRule.validate(urlFormEncoded) must
        be(Invalid(Seq(
          (Path \ "ukPassport") -> Seq(ValidationError("error.invalid"))
        )))
    }

    "JSON" must {

      "Read the json and return UKPassportNumber" in {
        val model = UKPassportYes("AA0000000")
        UKPassport.jsonReads.reads(UKPassport.jsonWrites.writes(model)) must
          be(JsSuccess(model, JsPath \ "ukPassportNumber"))
      }

      "Read the json and return no UKPassport" in {
        val model = UKPassportNo
        UKPassport.jsonReads.reads(UKPassport.jsonWrites.writes(model)) must
          be(JsSuccess(model, JsPath))
      }

      "Read the json and return error if passport number is missing" in {
        val json = Json.obj("ukPassport" -> true)
        UKPassport.jsonReads.reads(json) must be(
          JsError((JsPath  \ "ukPassportNumber") -> play.api.data.validation.ValidationError("error.path.missing"))
        )
      }
    }
  }

}
