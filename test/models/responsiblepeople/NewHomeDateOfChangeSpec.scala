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
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class NewHomeDateOfChangeSpec extends PlaySpec {
  "NewHomeDateOfChange" must {

    "read the form correctly when given a valid date" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("24"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("1990")
      )

      NewHomeDateOfChange.formRule.validate(model) must be (Valid(NewHomeDateOfChange(Some(new LocalDate(1990, 2, 24)))))

    }

    "write form data correctly" in {
      val model = Map (
        "dateOfChange.day" -> Seq(""),
        "dateOfChange.month" -> Seq(""),
        "dateOfChange.year" -> Seq("")
      )

      NewHomeDateOfChange.formWrites.writes(NewHomeDateOfChange(None)) must be(model)

    }

    "fail form validation when given a future date" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("24"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq(LocalDate.now().plusYears(1).getYear.toString)
      )

      NewHomeDateOfChange.formRule.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("new.home.error.required.date.future")))
        ))

    }

    "fail form validation when given a date with no day" in {
      val model = Map (
        "dateOfChange.day" -> Seq(""),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("2012")
      )

      NewHomeDateOfChange.formRule.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("new.home.error.required.date.day")))
        ))
    }

    "fail form validation when given a date with no month" in {
      val model = Map (
        "dateOfChange.day" -> Seq("2"),
        "dateOfChange.month" -> Seq(""),
        "dateOfChange.year" -> Seq("2012")
      )

      NewHomeDateOfChange.formRule.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("new.home.error.required.date.month")))
        ))
    }

    "fail form validation when given a date with no year" in {
      val model = Map (
        "dateOfChange.day" -> Seq("2"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("")
      )

      NewHomeDateOfChange.formRule.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("new.home.error.required.date.year")))
        ))
    }

    "fail form validation when given a date with no day and month" in {
      val model = Map (
        "dateOfChange.day" -> Seq(""),
        "dateOfChange.month" -> Seq(""),
        "dateOfChange.year" -> Seq("2012")
      )

      NewHomeDateOfChange.formRule.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("new.home.error.required.date.month.day")))
        ))
    }

    "fail form validation when given a date with no day and year" in {
      val model = Map (
        "dateOfChange.day" -> Seq(""),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("")
      )

      NewHomeDateOfChange.formRule.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("new.home.error.required.date.year.day")))
        ))
    }

    "fail form validation when given a date with no data" in {
      val model = Map (
        "dateOfChange.day" -> Seq(""),
        "dateOfChange.month" -> Seq(""),
        "dateOfChange.year" -> Seq("")
      )

      NewHomeDateOfChange.formRule.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("new.home.error.required.date.year.month.day")))
        ))
    }

    "Read and write successfully" in {
      NewHomeDateOfChange.format.reads(NewHomeDateOfChange.format.writes(NewHomeDateOfChange(Some(new LocalDate(1990, 2, 24))))) must be(
        JsSuccess(NewHomeDateOfChange(Some(new LocalDate(1990, 2, 24))), JsPath))

    }

    "write successfully" in {
      NewHomeDateOfChange.format.writes(NewHomeDateOfChange(Some(new LocalDate(1990, 2, 24)))) must be(Json.obj("dateOfChange" ->"1990-02-24"))
    }
  }
}
