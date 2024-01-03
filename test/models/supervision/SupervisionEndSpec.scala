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

package models.supervision

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.joda.time.LocalDate
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class SupervisionEndSpec extends PlaySpec with MockitoSugar {
  trait Fixture {

    val startDateField = Map("extraStartDate" -> Seq("2000-01-01"))
    val end = new LocalDate(2005, 2, 24)//scalastyle:off magic.number
  }

  "Form Rules and Writes" must {
    "given 'yes' selected with valid end date " in new Fixture {

      val urlFormEncoded = startDateField ++ Map(
        "anotherBody" -> Seq("true"),
        "endDate.day" -> Seq("24"),
        "endDate.month" -> Seq("2"),
        "endDate.year" -> Seq("2005")
      )

      val expected = Valid(SupervisionEnd(end))

      SupervisionEnd.formRule.validate(urlFormEncoded) must be(expected)
    }

    "fail validation" when {
      "required fields are missing" when {
        "nothing has been selected" in new Fixture{

          SupervisionEnd.formRule.validate(startDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "endDate.year" -> Seq(""),
            "endDate.month" -> Seq(""),
            "endDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "endDate") -> Seq(ValidationError("error.supervision.end.required.date.year.month.day"))
          )))
        }

        "day is missing" in new Fixture {
          SupervisionEnd.formRule.validate(startDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "endDate.year" -> Seq("2020"),
            "endDate.month" -> Seq("01"),
            "endDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "endDate") -> Seq(ValidationError("error.supervision.end.required.date.day"))
          )))
        }

        "month is missing" in new Fixture {
          SupervisionEnd.formRule.validate(startDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "endDate.year" -> Seq("2020"),
            "endDate.month" -> Seq(""),
            "endDate.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "endDate") -> Seq(ValidationError("error.supervision.end.required.date.month"))
          )))
        }

        "year is missing" in new Fixture {
          SupervisionEnd.formRule.validate(startDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "endDate.year" -> Seq(""),
            "endDate.month" -> Seq("01"),
            "endDate.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "endDate") -> Seq(ValidationError("error.supervision.end.required.date.year"))
          )))
        }

        "day and month are missing" in new Fixture {
          SupervisionEnd.formRule.validate(startDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "endDate.year" -> Seq("2020"),
            "endDate.month" -> Seq(""),
            "endDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "endDate") -> Seq(ValidationError("error.supervision.end.required.date.month.day"))
          )))
        }

        "day and year are missing" in new Fixture {
          SupervisionEnd.formRule.validate(startDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "endDate.year" -> Seq(""),
            "endDate.month" -> Seq("01"),
            "endDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "endDate") -> Seq(ValidationError("error.supervision.end.required.date.year.day"))
          )))
        }

        "year and month are missing" in new Fixture {
          SupervisionEnd.formRule.validate(startDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "endDate.year" -> Seq(""),
            "endDate.month" -> Seq(""),
            "endDate.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "endDate") -> Seq(ValidationError("error.supervision.end.required.date.year.month"))
          )))
        }
      }

      "Not a real date" in new Fixture {
        SupervisionEnd.formRule.validate(startDateField ++ Map(
          "anotherBody" -> Seq("true"),
          "endDate.year" -> Seq("FOO"),
          "endDate.month" -> Seq("BAR"),
          "endDate.day" -> Seq("FOO")
        )) must equal(Invalid(Seq(
          (Path \ "endDate") -> Seq(ValidationError("error.supervision.end.invalid.date.not.real"))
        )))
      }

      "Future date" in new Fixture {
        SupervisionEnd.formRule.validate(startDateField ++ Map(
          "anotherBody" -> Seq("true"),
          "endDate.year" -> Seq("2090"),
          "endDate.month" -> Seq("02"),
          "endDate.day" -> Seq("24")
        )) must
          equal(Invalid(Seq(
            (Path \ "endDate") -> Seq(ValidationError("error.supervision.end.invalid.date.future"))
          )))
      }

      "Pre 1900" in new Fixture {
        SupervisionEnd.formRule.validate(startDateField ++ Map(
          "anotherBody" -> Seq("true"),
          "endDate.year" -> Seq("1890"),
          "endDate.month" -> Seq("02"),
          "endDate.day" -> Seq("24")
        )) must
          equal(Invalid(Seq(
            (Path \ "endDate") -> Seq(ValidationError("error.supervision.end.invalid.date.after.1900"))
          )))
      }
    }
  }

  "Json read and writes" must {
    "successfully write end date" in  new Fixture {

      val expected = Map(
        "anotherBody" -> Seq("true"),
        "endDate.day" -> Seq("24"),
        "endDate.month" -> Seq("2"),
        "endDate.year" -> Seq("2005")
      )

      val input = SupervisionEnd(end)

      SupervisionEnd.formWrites.writes(input) must be(expected)
    }

    "Serialise SupervisionEnd as expected" in new Fixture {

      val input = SupervisionEnd(end)

      val expectedJson = Json.obj("supervisionEndDate" -> "2005-02-24")

      Json.toJson(input) must be(expectedJson)
    }

    "Deserialise SupervisionEnd as expected" in new Fixture {

      val input = Json.obj(
        "anotherBody" -> true,
        "supervisionEndDate" -> "2005-02-24")

      val expected = SupervisionEnd(end)

      Json.fromJson[SupervisionEnd](input) must be (JsSuccess(expected, JsPath \ "supervisionEndDate"))
    }

    "fail when data is missing" in {
      Json.fromJson[SupervisionEnd](Json.obj()) must
        be(JsError((JsPath \ "supervisionEndDate") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }
  }
}
