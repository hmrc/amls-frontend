/*
 * Copyright 2018 HM Revenue & Customs
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

class NonUKPassportSpec extends PlaySpec {

  "NonUk passport number" must {
    "pass validation" when {
      "given the correct number of numbers" in {
        NonUKPassport.noUKPassportType.validate("ab3456789") mustBe Valid("ab3456789")
      }
    }

    "fail validation" when {
      "the passport number has too many characters" in {
        NonUKPassport.noUKPassportType.validate("a" * 50) mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.invalid.non.uk.passport")))
        )
      }
      "the passport number includes invalid characters (letters, punctuation etc)" in {
        NonUKPassport.noUKPassportType.validate("123abc7{}") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.invalid.non.uk.passport")))
        )
      }
      "the passport number is an empty string" in {
        NonUKPassport.noUKPassportType.validate("") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.non.uk.passport")))
        )
      }
      "the passport number is given a sequence of whitespace" in {
        NonUKPassport.noUKPassportType.validate("    ") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.non.uk.passport")))
        )
      }
    }
  }


  "NonUKPassport" must {

    "pass validation for NoPassport" in {
      val urlFormEncoded = Map("nonUKPassport" -> Seq("false"))
      NonUKPassport.formRule.validate(urlFormEncoded) must be(Valid(NoPassport))
    }

    "pass validation for non uk passport number" in {
      val urlFormEncoded = Map(
        "nonUKPassport" -> Seq("true"),
        "nonUKPassportNumber" -> Seq("AA1234567")
      )
      NonUKPassport.formRule.validate(urlFormEncoded) must be(Valid(NonUKPassportYes("AA1234567")))
    }

    "fail validation if nonUKPassportNumber is empty" in {
      val urlFormEncoded = Map(
        "nonUKPassport" -> Seq("true"),
        "nonUKPassportNumber" -> Seq("")
      )
      NonUKPassport.formRule.validate(urlFormEncoded) must be(Invalid(Seq(
        (Path \ "nonUKPassportNumber") -> Seq(ValidationError("error.required.non.uk.passport"))
      )))
    }

    "write correct no UKPassport model" in {
      val data = Map(
        "nonUKPassport" -> Seq("true"),
        "nonUKPassportNumber" -> Seq("AA1234567")
      )
      NonUKPassport.formWrites.writes(NonUKPassportYes("AA1234567")) must be(data)

    }

    "write correct no NoPassport model" in {
      val data = Map(
        "nonUKPassport" -> Seq("false")
      )
      NonUKPassport.formWrites.writes(NoPassport) must be(data)
    }

    "fail to validate given an invalid value" in {

      val urlFormEncoded = Map("nonUKPassport" -> Seq("10"))

      NonUKPassport.formRule.validate(urlFormEncoded) must
        be(Invalid(Seq(
          (Path \ "nonUKPassport") -> Seq(ValidationError("error.required.select.non.uk.passport"))
        )))
    }

    "JSON" must {

      "Read the json and return the PassportType domain object successfully for the NoPassport" in {

        NonUKPassport.jsonReads.reads(NonUKPassport.jsonWrites.writes(NoPassport)) must
          be(JsSuccess(NoPassport, JsPath))
      }

      "Read the json and return NonUKPassport" in {
        val model = NonUKPassportYes("21321313213132132")
        NonUKPassport.jsonReads.reads(NonUKPassport.jsonWrites.writes(model)) must
          be(JsSuccess(model, JsPath \ "nonUKPassportNumber"))
      }

      "Read the json and return error if passport number is missing" in {
        val json = Json.obj("nonUKPassport" -> true)
        NonUKPassport.jsonReads.reads(json) must be(
          JsError((JsPath  \ "nonUKPassportNumber") -> play.api.data.validation.ValidationError("error.path.missing"))
        )
      }
    }
  }


}
