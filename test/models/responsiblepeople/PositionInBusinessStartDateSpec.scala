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

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class PositionInBusinessStartDateSpec extends PlaySpec with MockitoSugar {

  "PositionStartDate" should {

    "successfully validate given a valid date" in {

      val data = Map(
        "startDate.day" -> Seq("15"),
        "startDate.month" -> Seq("2"),
        "startDate.year" -> Seq("1956")
      )

      PositionStartDate.formRule.validate(data) must
        be(Valid(PositionStartDate(new LocalDate(1956, 2, 15))))
    }

    "fail validation" when {
      "required fields are missing" when {
        "nothing has been selected" in {

          PositionStartDate.formRule.validate(Map(
            "startDate.year" -> Seq(""),
            "startDate.month" -> Seq(""),
            "startDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.rp.position.required.date.year.month.day"))
          )))
        }

        "day is missing" in {
          PositionStartDate.formRule.validate(Map(
            "startDate.year" -> Seq("2020"),
            "startDate.month" -> Seq("01"),
            "startDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.rp.position.required.date.day"))
          )))
        }

        "month is missing" in {
          PositionStartDate.formRule.validate(Map(
            "startDate.year" -> Seq("2020"),
            "startDate.month" -> Seq(""),
            "startDate.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.rp.position.required.date.month"))
          )))
        }

        "year is missing" in {
          PositionStartDate.formRule.validate(Map(
            "startDate.year" -> Seq(""),
            "startDate.month" -> Seq("01"),
            "startDate.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.rp.position.required.date.year"))
          )))
        }

        "day and month are missing" in {
          PositionStartDate.formRule.validate(Map(
            "startDate.year" -> Seq("2020"),
            "startDate.month" -> Seq(""),
            "startDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.rp.position.required.date.month.day"))
          )))
        }

        "day and year are missing" in {
          PositionStartDate.formRule.validate(Map(
            "startDate.year" -> Seq(""),
            "startDate.month" -> Seq("01"),
            "startDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.rp.position.required.date.year.day"))
          )))
        }

        "year and month are missing" in {
          PositionStartDate.formRule.validate(Map(
            "startDate.year" -> Seq(""),
            "startDate.month" -> Seq(""),
            "startDate.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.rp.position.required.date.year.month"))
          )))
        }
      }

      "Not a real date" in {
        PositionStartDate.formRule.validate(Map(
          "startDate.year" -> Seq("FOO"),
          "startDate.month" -> Seq("BAR"),
          "startDate.day" -> Seq("FOO")
        )) must equal(Invalid(Seq(
          (Path \ "startDate") -> Seq(ValidationError("error.rp.position.invalid.date.not.real"))
        )))
      }

      "Future date" in {
        PositionStartDate.formRule.validate(Map(
          "startDate.year" -> Seq("2090"),
          "startDate.month" -> Seq("02"),
          "startDate.day" -> Seq("24")
        )) must
          equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.rp.position.invalid.date.future"))
          )))
      }

      "Pre 1900" in {
        PositionStartDate.formRule.validate(Map(
          "startDate.year" -> Seq("1890"),
          "startDate.month" -> Seq("02"),
          "startDate.day" -> Seq("24")
        )) must
          equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.rp.position.invalid.date.after.1900"))
          )))
      }
    }

    "write to form given a valid start date" in {

      val startDate = PositionStartDate(new LocalDate(1990, 2, 24))

      PositionStartDate.formWrites.writes(startDate) must
        be(Map(
          "startDate.day" -> List("24"), "startDate.month" -> List("2"), "startDate.year" -> List("1990")))
    }
  }
}
