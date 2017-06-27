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

    "throw error message" when {
      "day entered is invalid" in {
        val errorDayModel = Map(
          "dateOfBirth.day" -> Seq("2466"),
          "dateOfBirth.month" -> Seq("2"),
          "dateOfBirth.year" -> Seq("1990")
        )

        DateOfBirth.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "dateOfBirth" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "month entered is invalid" in {
        val errorDayModel = Map(
          "dateOfBirth.day" -> Seq("24"),
          "dateOfBirth.month" -> Seq("29"),
          "dateOfBirth.year" -> Seq("1990")
        )

        DateOfBirth.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "dateOfBirth" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "year entered is too long" in {
        val errorDayModel = Map(
          "dateOfBirth.day" -> Seq("24"),
          "dateOfBirth.month" -> Seq("11"),
          "dateOfBirth.year" -> Seq("199000")
        )

        DateOfBirth.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "dateOfBirth" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "year entered is too short" in {
        val errorDayModel = Map(
          "dateOfBirth.day" -> Seq("24"),
          "dateOfBirth.month" -> Seq("11"),
          "dateOfBirth.year" -> Seq("16")
        )

        DateOfBirth.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "dateOfBirth" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "all fields are empty" in {
        val noContentModel = Map(
          "dateOfBirth.day" -> Seq(""),
          "dateOfBirth.month" -> Seq(""),
          "dateOfBirth.year" -> Seq("")
        )

        DateOfBirth.formRule.validate(noContentModel) must be(
          Invalid(Seq(Path \ "dateOfBirth" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))))
        )
      }
    }
  }

  "DateOfBirth Json" must {

    "Read and write successfully" in {

      DateOfBirth.format.reads(
        DateOfBirth.format.writes(DateOfBirth(new LocalDate(1990, 2, 24)))
      ) must be(
        JsSuccess(DateOfBirth(new LocalDate(1990, 2, 24)), JsPath \ "dateOfBirth")
      )

    }

    "write successfully" in {
      DateOfBirth.format.writes(DateOfBirth(new LocalDate(1990, 2, 24))) must be(
        Json.obj("dateOfBirth" -> "1990-02-24")
      )
    }
  }

}
