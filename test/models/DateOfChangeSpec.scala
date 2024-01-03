/*
 * Copyright 2024 HM Revenue & Customs
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

package models

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsString, _}


class DateOfChangeSpec extends PlaySpec {
  "DateOfChange" must {

    "read the form correctly when given a valid date" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("24"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("1990")
      )

      DateOfChange.formRule.validate(model) must be (Valid(DateOfChange(new LocalDate(1990, 2, 24))))

    }

    "fail form validation when given a future date" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("24"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq(LocalDate.now().plusYears(1).getYear.toString)
      )

      DateOfChange.formRule.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.future.date")))
        ))

    }

    "fail form validation when given a future date in pre-app" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("24"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq(LocalDate.now().plusYears(1).getYear.toString)
      )

      DateOfChange.formRulePreApp.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.required.dateofchange.future")))
        ))

    }

    "fail form validation when given a fake date in pre-app" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("244"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("1900")
      )

      DateOfChange.formRulePreApp.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.required.dateofchange.not.real")))
        ))

    }

    "fail form validation when given a past 1900 date in pre-app" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("24"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("1800")
      )

      DateOfChange.formRulePreApp.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.required.dateofchange.1900")))
        ))
    }

    "fail form validation when missing day field in pre-app" in {
      val model =    Map (
        "dateOfChange.day" -> Seq(""),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("2000")
      )

      DateOfChange.formRulePreApp.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.required.dateofchange.day")))
        ))
    }

    "fail form validation when missing month field in pre-app" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("2"),
        "dateOfChange.month" -> Seq(""),
        "dateOfChange.year" -> Seq("2000")
      )

      DateOfChange.formRulePreApp.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.required.dateofchange.month")))
        ))
    }

    "fail form validation when missing year field in pre-app" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("2"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("")
      )

      DateOfChange.formRulePreApp.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.required.dateofchange.year")))
        ))
    }

    "fail form validation when missing day and month field in pre-app" in {
      val model =    Map (
        "dateOfChange.day" -> Seq(""),
        "dateOfChange.month" -> Seq(""),
        "dateOfChange.year" -> Seq("2000")
      )

      DateOfChange.formRulePreApp.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.required.dateofchange.month.day")))
        ))
    }

    "fail form validation when missing day and year field in pre-app" in {
      val model =    Map (
        "dateOfChange.day" -> Seq(""),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("")
      )

      DateOfChange.formRulePreApp.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.required.dateofchange.year.day")))
        ))
    }

    "fail form validation when missing month and year field in pre-app" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("2"),
        "dateOfChange.month" -> Seq(""),
        "dateOfChange.year" -> Seq("")
      )

      DateOfChange.formRulePreApp.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.required.dateofchange.year.month")))
        ))
    }

    "fail form validation when given a date before a business activities start date" in {
      val model = Map (
        "dateOfChange.day" -> Seq("24"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("2012"),
        "activityStartDate" -> Seq("2016-05-25")
      )

      DateOfChange.formRule.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.expected.dateofchange.date.after.activitystartdate", "25-05-2016")))
        ))
    }

    "read from JSON correctly" in {

      val json = JsString("2016-02-24")

      val result = Json.fromJson[DateOfChange](json)
      result.get.dateOfChange must be(new LocalDate(2016,2,24))
    }

    "write to JSON correctly" in {

      val date = DateOfChange(new LocalDate(2016,2,24))
      val json = JsString("2016-02-24")

      val result = Json.toJson(date)
      result must be(json)
    }
  }
}
