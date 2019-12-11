/*
 * Copyright 2019 HM Revenue & Customs
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

package models.businessdetails

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import jto.validation.Path
import cats.data.Validated.{Invalid, Valid}
import jto.validation.ValidationError
import models.tradingpremises.ActivityStartDate
import play.api.libs.json.{JsPath, JsSuccess, Json}


class ActivityStartDateSpec extends PlaySpec {
  // scalastyle:off

  "Form validation" must {
    "pass validation" when {
      "given a valid date" in {

        val model = Map(
          "startDate.day" -> Seq("24"),
          "startDate.month" -> Seq("2"),
          "startDate.year" -> Seq("1990")
        )

        ActivityStartDate.formRule.validate(model) must be(Valid(ActivityStartDate(new LocalDate(1990, 2, 24))))
      }
    }

  "fail validation" when {
    "given a day in future beyond end of 2099" in {
      val model = Map(
        "startDate.day" -> Seq("1"),
        "startDate.month" -> Seq("1"),
        "startDate.year" -> Seq("2100")
      )
      ActivityStartDate.formRule.validate(model) must be(Invalid(Seq(
        Path \ "startDate" -> Seq(ValidationError("error.invalid.date.past"))
      )))
    }
  }

  "fail validation" when {
    "given a day in the past before start of 1900" in {
      val model = Map(
        "startDate.day" -> Seq("31"),
        "startDate.month" -> Seq("12"),
        "startDate.year" -> Seq("1899")
      )
      ActivityStartDate.formRule.validate(model) must be(Invalid(Seq(
        Path \ "startDate" -> Seq(ValidationError("error.invalid.date.after.1900"))
      )))
    }
  }

  "fail validation" when {
    "given a future date" in {

      val model = Map(
        "startDate.day" -> Seq("1"),
        "startDate.month" -> Seq("1"),
        "startDate.year" -> Seq("2050")
      )
      ActivityStartDate.formRule.validate(model) must be(Invalid(Seq(
        Path \ "startDate" -> Seq(ValidationError("error.invalid.date.past"))
      )))
    }
  }

    "fail validation" when {
      "given a day value with too many numerical characters" in {

        val model = Map(
          "startDate.day" -> Seq("2466"),
          "startDate.month" -> Seq("2"),
          "startDate.year" -> Seq("1990")
        )

        ActivityStartDate.formRule.validate(model) must be(Invalid(Seq(Path \ "startDate" -> Seq(
          ValidationError("error.invalid.date.not.real")))))
      }

      "given missing data represented by an empty string" in {

        val model = Map(
          "startDate.day" -> Seq(""),
          "startDate.month" -> Seq(""),
          "startDate.year" -> Seq("")
        )
        ActivityStartDate.formRule.validate(model) must be(Invalid(Seq(
          Path \ "startDate" -> Seq(ValidationError("error.required.date.year.month.day"))
        )))
      }

      "given missing data represented by an empty Map" in {

        ActivityStartDate.formRule.validate(Map.empty) must be(Invalid(Seq(
          Path \ "startDate" -> Seq(ValidationError("error.required")),
          Path \ "startDate" -> Seq(ValidationError("error.required")),
          Path \ "startDate" -> Seq(ValidationError("error.required"))
        )))
      }
    }

    "successfully write the model" in {

      ActivityStartDate.formWrites.writes(ActivityStartDate(new LocalDate(1990, 2, 24))) mustBe Map(
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("2"),
        "startDate.year" -> Seq("1990")
      )
    }
  }

  "Json validation" must {

    "Read and write successfully" in {

      ActivityStartDate.format.reads(ActivityStartDate.format.writes(ActivityStartDate(new LocalDate(1990, 2, 24)))) must be(
        JsSuccess(ActivityStartDate(new LocalDate(1990, 2, 24)), JsPath \ "startDate"))
    }

    "write successfully" in {
      ActivityStartDate.format.writes(ActivityStartDate(new LocalDate(1990, 2, 24))) must be(Json.obj("startDate" -> "1990-02-24"))
    }
  }
}
