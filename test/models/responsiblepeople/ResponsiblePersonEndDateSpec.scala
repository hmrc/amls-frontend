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

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}


class ResponsiblePersonEndDateSpec extends PlaySpec {

  val validYear = 1990
  val validDay = 24
  val validMonth = 2

  val standardExtraData = Map(
    "positionStartDate" -> Seq("1990-12-01"),
    "userName" -> Seq("User 1")
  )

  "ResponsiblePersonEndDate Form" must {
    "successfully read the model" in {

      val validModel = Map(
        "positionStartDate" -> Seq("1988-12-01"),
        "userName" -> Seq("User 1"),
        "endDate.day" -> Seq("24"),
        "endDate.month" -> Seq("2"),
        "endDate.year" -> Seq("1990")
      )

      ResponsiblePersonEndDate.formRule.validate(validModel) must be(
        Valid(ResponsiblePersonEndDate(new LocalDate(validYear, validMonth, validDay))))
    }

    "successfully write the model" in {
      ResponsiblePersonEndDate.formWrites.writes(ResponsiblePersonEndDate(new LocalDate(validYear, validMonth, validDay))) must be(
        Map(
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1990")
        ))
    }

    "throw error message" when {
      "day entered is invalid" in {
        val errorDayModel = standardExtraData ++ Map(
          "endDate.day" -> Seq("2466"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1990")
        )

        ResponsiblePersonEndDate.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "endDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "month entered is invalid" in {
        val errorDayModel = standardExtraData ++ Map(
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("29"),
          "endDate.year" -> Seq("1990")
        )

        ResponsiblePersonEndDate.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "endDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "year entered is too long" in {
        val errorDayModel = standardExtraData ++ Map(
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("11"),
          "endDate.year" -> Seq("199000")
        )

        ResponsiblePersonEndDate.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "endDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "year entered is too short" in {
        val errorDayModel = standardExtraData ++ Map(
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("11"),
          "endDate.year" -> Seq("16")
        )

        ResponsiblePersonEndDate.formRule.validate(errorDayModel) must be(
          Invalid(Seq(Path \ "endDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "all fields are empty" in {
        val noContentModel = standardExtraData ++ Map(
          "endDate.day" -> Seq(""),
          "endDate.month" -> Seq(""),
          "endDate.year" -> Seq("")
        )

        ResponsiblePersonEndDate.formRule.validate(noContentModel) must be(
          Invalid(Seq(Path \ "endDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))))
        )
      }
    }
  }

  "ResponsiblePersonEndDate Json" must {

    "Read and write successfully" in {

      ResponsiblePersonEndDate.format.reads(
        ResponsiblePersonEndDate.format.writes(ResponsiblePersonEndDate(new LocalDate(1990, 2, 24)))) must be(
        JsSuccess(ResponsiblePersonEndDate(new LocalDate(1990, 2, 24)), JsPath \ "endDate"))

    }

    "write successfully" in {
      ResponsiblePersonEndDate.format.writes(ResponsiblePersonEndDate(new LocalDate(1990, 2, 24))) must be(Json.obj("endDate" -> "1990-02-24"))
    }
  }

}
