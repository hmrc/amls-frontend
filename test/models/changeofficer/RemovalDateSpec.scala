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

package models.changeofficer

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class RemovalDateSpec extends PlaySpec {

  val validYear = 1990
  val validDay = 24
  val validMonth = 2

  "RemovalDate" must {
    "successfully read the model" in {

      val validModel = Map(
        "date.day" -> Seq("24"),
        "date.month" -> Seq("2"),
        "date.year" -> Seq("1990")
      )

      RemovalDate.formRule.validate(validModel) must be(
        Valid(RemovalDate(new LocalDate(validYear, validMonth, validDay))))
    }

    "successfully write the model" in {
      RemovalDate.formWrites.writes(RemovalDate(new LocalDate(validYear, validMonth, validDay))) must be(
        Map(
          "date.day" -> Seq("24"),
          "date.month" -> Seq("2"),
          "date.year" -> Seq("1990")
        ))
    }

    "throw error message" when {
      "day entered is invalid" in {
        val errorDayModel = Map(
          "date.day" -> Seq("2466"),
          "date.month" -> Seq("2"),
          "date.year" -> Seq("1990")
        )

        RemovalDate.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "date" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "month entered is invalid" in {
        val errorDayModel = Map(
          "date.day" -> Seq("24"),
          "date.month" -> Seq("29"),
          "date.year" -> Seq("1990")
        )

        RemovalDate.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "date" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "year entered is too long" in {
        val errorDayModel = Map(
          "date.day" -> Seq("24"),
          "date.month" -> Seq("11"),
          "date.year" -> Seq("199000")
        )

        RemovalDate.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "date" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "year entered is too short" in {
        val errorDayModel = Map(
          "date.day" -> Seq("24"),
          "date.month" -> Seq("11"),
          "date.year" -> Seq("16")
        )

        RemovalDate.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "date" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "all fields are empty" in {
        val noContentModel = Map(
          "date.day" -> Seq(""),
          "date.month" -> Seq(""),
          "date.year" -> Seq("")
        )

        RemovalDate.formRule.validate(noContentModel) must be(
          Invalid(Seq(Path \ "date" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))))
        )
      }
    }
  }

}
