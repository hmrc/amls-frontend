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

package models.responsiblepeople

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class DateOfBirthSpec extends PlaySpec {

  val validYear = 1990
  val validDay = 24
  val validMonth = 2

  "DateOfBirth" must {
    "successfully read the model" in {

      val validModel = Map(
        "dateOfBirth.day" -> Seq("24"),
        "dateOfBirth.month" -> Seq("2"),
        "dateOfBirth.year" -> Seq("1990")
      )

      DateOfBirth.formRule.validate(validModel) must be(
        Valid(DateOfBirth(new LocalDate(validYear, validMonth, validDay))))
    }

    "successfully write the model" in {
      DateOfBirth.formWrites.writes(DateOfBirth(new LocalDate(validYear, validMonth, validDay))) must be(
        Map(
          "dateOfBirth.day" -> Seq("24"),
          "dateOfBirth.month" -> Seq("2"),
          "dateOfBirth.year" -> Seq("1990")
        ))
    }

    "fail validation" when {
      "required fields are missing" when {
        "nothing has been selected" in {

          DateOfBirth.formRule.validate(Map(
            "dateOfBirth.year" -> Seq(""),
            "dateOfBirth.month" -> Seq(""),
            "dateOfBirth.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "dateOfBirth") -> Seq(ValidationError("error.rp.dob.required.date.year.month.day"))
          )))
        }

        "day is missing" in {
          DateOfBirth.formRule.validate(Map(
            "dateOfBirth.year" -> Seq("2020"),
            "dateOfBirth.month" -> Seq("01"),
            "dateOfBirth.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "dateOfBirth") -> Seq(ValidationError("error.rp.dob.required.date.day"))
          )))
        }

        "month is missing" in {
          DateOfBirth.formRule.validate(Map(
            "dateOfBirth.year" -> Seq("2020"),
            "dateOfBirth.month" -> Seq(""),
            "dateOfBirth.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "dateOfBirth") -> Seq(ValidationError("error.rp.dob.required.date.month"))
          )))
        }

        "year is missing" in {
          DateOfBirth.formRule.validate(Map(
            "dateOfBirth.year" -> Seq(""),
            "dateOfBirth.month" -> Seq("01"),
            "dateOfBirth.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "dateOfBirth") -> Seq(ValidationError("error.rp.dob.required.date.year"))
          )))
        }

        "day and month are missing" in {
          DateOfBirth.formRule.validate(Map(
            "dateOfBirth.year" -> Seq("2020"),
            "dateOfBirth.month" -> Seq(""),
            "dateOfBirth.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "dateOfBirth") -> Seq(ValidationError("error.rp.dob.required.date.month.day"))
          )))
        }

        "day and year are missing" in {
          DateOfBirth.formRule.validate(Map(
            "dateOfBirth.year" -> Seq(""),
            "dateOfBirth.month" -> Seq("01"),
            "dateOfBirth.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "dateOfBirth") -> Seq(ValidationError("error.rp.dob.required.date.year.day"))
          )))
        }

        "year and month are missing" in {
          DateOfBirth.formRule.validate(Map(
            "dateOfBirth.year" -> Seq(""),
            "dateOfBirth.month" -> Seq(""),
            "dateOfBirth.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "dateOfBirth") -> Seq(ValidationError("error.rp.dob.required.date.year.month"))
          )))
        }
      }

      "Not a real date" in {
        DateOfBirth.formRule.validate(Map(
          "dateOfBirth.year" -> Seq("FOO"),
          "dateOfBirth.month" -> Seq("BAR"),
          "dateOfBirth.day" -> Seq("FOO")
        )) must equal(Invalid(Seq(
          (Path \ "dateOfBirth") -> Seq(ValidationError("error.rp.dob.invalid.date.not.real"))
        )))
      }

      "Future date" in {
        DateOfBirth.formRule.validate(Map(
          "dateOfBirth.year" -> Seq("2090"),
          "dateOfBirth.month" -> Seq("02"),
          "dateOfBirth.day" -> Seq("24")
        )) must
          equal(Invalid(Seq(
            (Path \ "dateOfBirth") -> Seq(ValidationError("error.rp.dob.invalid.date.future"))
          )))
      }

      "Pre 1900" in {
        DateOfBirth.formRule.validate(Map(
          "dateOfBirth.year" -> Seq("1890"),
          "dateOfBirth.month" -> Seq("02"),
          "dateOfBirth.day" -> Seq("24")
        )) must
          equal(Invalid(Seq(
            (Path \ "dateOfBirth") -> Seq(ValidationError("error.rp.dob.invalid.date.after.1900"))
          )))
      }
    }
  }

  "DateOfBirth Json" must {

    "Read and write successfully" in {

      DateOfBirth.format.reads(
        DateOfBirth.format.writes(DateOfBirth(new LocalDate(1990, 2, 24)))
      ) must be(
        JsSuccess(DateOfBirth(new LocalDate(1990, 2, 24)), JsPath)
      )

    }

    "write successfully" in {
      DateOfBirth.format.writes(DateOfBirth(new LocalDate(1990, 2, 24))) must be(
        Json.obj("dateOfBirth" -> "1990-02-24")
      )
    }
  }

}
