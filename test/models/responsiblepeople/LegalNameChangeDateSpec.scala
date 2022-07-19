/*
 * Copyright 2022 HM Revenue & Customs
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
import org.joda.time.LocalDate
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

@SuppressWarnings(Array("org.brianmckenna.wartremover.warts.MutableDataStructures"))
class LegalNameChangeDateSpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given all fields" in {

        val data = Map(
          "date.year" -> Seq("1990"),
          "date.month" -> Seq("02"),
          "date.day" -> Seq("24")
        )

        val validDate = LegalNameChangeDate(
          date = new LocalDate(1990, 2, 24)
        )

        LegalNameChangeDate.formRule.validate(data) must equal(Valid(validDate))
      }

    }

    "fail validation" when {
      "required fields are missing" when {
        "nothing has been selected" in {

          LegalNameChangeDate.formRule.validate(Map(
            "date.year" -> Seq(""),
            "date.month" -> Seq(""),
            "date.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "date") -> Seq(ValidationError("error.rp.name_change.required.date.year.month.day"))
          )))
        }

        "day is missing" in {
          LegalNameChangeDate.formRule.validate(Map(
            "date.year" -> Seq("2020"),
            "date.month" -> Seq("01"),
            "date.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "date") -> Seq(ValidationError("error.rp.name_change.required.date.day"))
          )))
        }

        "month is missing" in {
          LegalNameChangeDate.formRule.validate(Map(
            "date.year" -> Seq("2020"),
            "date.month" -> Seq(""),
            "date.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "date") -> Seq(ValidationError("error.rp.name_change.required.date.month"))
          )))
        }

        "year is missing" in {
          LegalNameChangeDate.formRule.validate(Map(
            "date.year" -> Seq(""),
            "date.month" -> Seq("01"),
            "date.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "date") -> Seq(ValidationError("error.rp.name_change.required.date.year"))
          )))
        }

        "day and month are missing" in {
          LegalNameChangeDate.formRule.validate(Map(
            "date.year" -> Seq("2020"),
            "date.month" -> Seq(""),
            "date.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "date") -> Seq(ValidationError("error.rp.name_change.required.date.month.day"))
          )))
        }

        "day and year are missing" in {
          LegalNameChangeDate.formRule.validate(Map(
            "date.year" -> Seq(""),
            "date.month" -> Seq("01"),
            "date.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "date") -> Seq(ValidationError("error.rp.name_change.required.date.year.day"))
          )))
        }

        "year and month are missing" in {
          LegalNameChangeDate.formRule.validate(Map(
            "date.year" -> Seq(""),
            "date.month" -> Seq(""),
            "date.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "date") -> Seq(ValidationError("error.rp.name_change.required.date.year.month"))
          )))
        }
      }

      "Not a real date" in {
        LegalNameChangeDate.formRule.validate(Map(
          "date.year" -> Seq("FOO"),
          "date.month" -> Seq("BAR"),
          "date.day" -> Seq("FOO")
        )) must equal(Invalid(Seq(
          (Path \ "date") -> Seq(ValidationError("error.rp.name_change.invalid.date.not.real"))
        )))
      }

      "Future date" in {
        LegalNameChangeDate.formRule.validate(Map(
          "date.year" -> Seq("2090"),
          "date.month" -> Seq("02"),
          "date.day" -> Seq("24")
        )) must
          equal(Invalid(Seq(
            (Path \ "date") -> Seq(ValidationError("error.rp.name_change.invalid.date.future"))
          )))
      }

      "Pre 1900" in {
        LegalNameChangeDate.formRule.validate(Map(
          "date.year" -> Seq("1890"),
          "date.month" -> Seq("02"),
          "date.day" -> Seq("24")
        )) must
          equal(Invalid(Seq(
            (Path \ "date") -> Seq(ValidationError("error.rp.name_change.invalid.date.after.1900"))
          )))
      }
    }
  }
}
