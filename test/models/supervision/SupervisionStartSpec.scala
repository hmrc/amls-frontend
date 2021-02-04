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

package models.supervision

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class SupervisionStartSpec extends PlaySpec with MockitoSugar {
  trait Fixture {

    val endDateField = Map("extraEndDate" -> Seq("2000-01-01"))
    val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
  }

  "Form Rules and Writes" must {
    "given 'yes' selected with valid start date " in new Fixture {

      val urlFormEncoded = endDateField ++ Map(
        "anotherBody" -> Seq("true"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("2"),
        "startDate.year" -> Seq("1990")
      )

      val expected = Valid(SupervisionStart(start))

      SupervisionStart.formRule.validate(urlFormEncoded) must be(expected)
    }

    "fail validation" when {
      "required fields are missing" when {
        "nothing has been selected" in new Fixture{

          SupervisionStart.formRule.validate(endDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "startDate.year" -> Seq(""),
            "startDate.month" -> Seq(""),
            "startDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.supervision.start.required.date.year.month.day"))
          )))
        }

        "day is missing" in new Fixture {
          SupervisionStart.formRule.validate(endDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "startDate.year" -> Seq("2020"),
            "startDate.month" -> Seq("01"),
            "startDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.supervision.start.required.date.day"))
          )))
        }

        "month is missing" in new Fixture {
          SupervisionStart.formRule.validate(endDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "startDate.year" -> Seq("2020"),
            "startDate.month" -> Seq(""),
            "startDate.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.supervision.start.required.date.month"))
          )))
        }

        "year is missing" in new Fixture {
          SupervisionStart.formRule.validate(endDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "startDate.year" -> Seq(""),
            "startDate.month" -> Seq("01"),
            "startDate.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.supervision.start.required.date.year"))
          )))
        }

        "day and month are missing" in new Fixture {
          SupervisionStart.formRule.validate(endDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "startDate.year" -> Seq("2020"),
            "startDate.month" -> Seq(""),
            "startDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.supervision.start.required.date.month.day"))
          )))
        }

        "day and year are missing" in new Fixture {
          SupervisionStart.formRule.validate(endDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "startDate.year" -> Seq(""),
            "startDate.month" -> Seq("01"),
            "startDate.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.supervision.start.required.date.year.day"))
          )))
        }

        "year and month are missing" in new Fixture {
          SupervisionStart.formRule.validate(endDateField ++ Map(
            "anotherBody" -> Seq("true"),
            "startDate.year" -> Seq(""),
            "startDate.month" -> Seq(""),
            "startDate.day" -> Seq("01")
          )) must equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.supervision.start.required.date.year.month"))
          )))
        }
      }

      "Not a real date" in new Fixture {
        SupervisionStart.formRule.validate(endDateField ++ Map(
          "anotherBody" -> Seq("true"),
          "startDate.year" -> Seq("FOO"),
          "startDate.month" -> Seq("BAR"),
          "startDate.day" -> Seq("FOO")
        )) must equal(Invalid(Seq(
          (Path \ "startDate") -> Seq(ValidationError("error.supervision.start.invalid.date.not.real"))
        )))
      }

      "Future date" in new Fixture {
        SupervisionStart.formRule.validate(endDateField ++ Map(
          "anotherBody" -> Seq("true"),
          "startDate.year" -> Seq("2090"),
          "startDate.month" -> Seq("02"),
          "startDate.day" -> Seq("24")
        )) must
          equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.supervision.start.invalid.date.future"))
          )))
      }

      "Pre 1900" in new Fixture {
        SupervisionStart.formRule.validate(endDateField ++ Map(
          "anotherBody" -> Seq("true"),
          "startDate.year" -> Seq("1890"),
          "startDate.month" -> Seq("02"),
          "startDate.day" -> Seq("24")
        )) must
          equal(Invalid(Seq(
            (Path \ "startDate") -> Seq(ValidationError("error.supervision.start.invalid.date.after.1900"))
          )))
      }
    }
  }

  "Json read and writes" must {
    "successfully write start date" in  new Fixture {

      val expected = Map(
        "anotherBody" -> Seq("true"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("2"),
        "startDate.year" -> Seq("1990")
      )

      val input = SupervisionStart(start)

      SupervisionStart.formWrites.writes(input) must be(expected)
    }

    "Serialise SupervisionStart as expected" in new Fixture {

      val input = SupervisionStart(start)

      val expectedJson = Json.obj("supervisionStartDate" -> "1990-02-24")

      Json.toJson(input) must be(expectedJson)
    }

    "Deserialise SupervisionStart as expected" in new Fixture {

      val input = Json.obj(
        "anotherBody" -> true,
        "supervisionStartDate" -> "1990-02-24")

      val expected = SupervisionStart(start)

      Json.fromJson[SupervisionStart](input) must be (JsSuccess(expected, JsPath \ "supervisionStartDate"))
    }

    "fail when data is missing" in {
      Json.fromJson[SupervisionStart](Json.obj()) must
        be(JsError((JsPath \ "supervisionStartDate") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }
  }
}
