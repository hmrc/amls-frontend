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

package models.supervision

import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class AnotherBodySpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given 'no' selected" in {
        val urlFormEncoded = Map("anotherBody" -> Seq("false"))
        val expected = Valid(AnotherBodyNo)
        AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
      }

      "given 'yes' selected with valid data" in {

        val urlFormEncoded = Map(
          "anotherBody" -> Seq("true"),
          "supervisorName" -> Seq("Name"),
          "startDate.day" -> Seq("24"),
          "startDate.month" -> Seq("2"),
          "startDate.year" -> Seq("1990"),
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1998"),
          "endingReason" -> Seq("Reason")
        )

        val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
        val end = new LocalDate(1998, 2, 24) //scalastyle:off magic.number
        val expected = Valid(AnotherBodyYes("Name", start, end, "Reason"))

        AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
      }
    }

    "fail validation" when {
      "given a future date" in {

        val data = AnotherBody.formWrites.writes(AnotherBodyYes("Name", LocalDate.now().plusDays(1), LocalDate.now().plusMonths(1), "Reason"))
        AnotherBody.formRule.validate(data) must be(Invalid(Seq(Path \ "startDate" -> Seq(
          ValidationError("error.future.date")),Path \ "endDate" -> Seq(
          ValidationError("error.future.date")))))
      }
    }

    "fail validation" when {
      "missing values when Yes selected" in {
        val urlFormEncoded = Map("anotherBody" -> Seq("true"))
        val expected = Invalid(
          Seq((Path \ "supervisorName") -> Seq(ValidationError("error.required")),
            (Path \ "startDate") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")),
            (Path \ "endDate") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")),
            (Path \ "endingReason") -> Seq(ValidationError("error.required")))
        )
        AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
      }

      "supervision enddate is before supervision startdate" in {
        val urlFormEncoded = Map(
          "anotherBody" -> Seq("true"),
          "supervisorName" -> Seq("Name"),
          "startDate.day" -> Seq("25"),
          "startDate.month" -> Seq("2"),
          "startDate.year" -> Seq("1998"),
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1998"),
          "endingReason" -> Seq("reason")
        )

        val expected = Invalid(
          Seq((Path \ "startDate") -> Seq(ValidationError("error.expected.supervision.startdate.before.enddate")),
          (Path \ "endDate") -> Seq(ValidationError("error.expected.supervision.enddate.after.startdate")))
        )

        AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
      }

      "given invalid characters in endingReason and supervisorName" in {
        val urlFormEncoded = Map(
          "anotherBody" -> Seq("true"),
          "supervisorName" -> Seq("invalid {} <>"),
          "startDate.day" -> Seq("24"),
          "startDate.month" -> Seq("2"),
          "startDate.year" -> Seq("1990"),
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1998"),
          "endingReason" -> Seq("invalid {} <>")
        )

        val expected = Invalid(
              Seq((Path \ "supervisorName") -> Seq(ValidationError("err.text.validation")),
                (Path \ "endingReason") -> Seq(ValidationError("err.text.validation")))
          )

        AnotherBody.formRule.validate(urlFormEncoded) must be(expected)

      }

      "given only spaces in endingReason and supervisorName" in {
        val urlFormEncoded = Map(
          "anotherBody" -> Seq("true"),
          "supervisorName" -> Seq("  "),
          "startDate.day" -> Seq("24"),
          "startDate.month" -> Seq("2"),
          "startDate.year" -> Seq("1990"),
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1998"),
          "endingReason" -> Seq("  ")
        )

        val expected = Invalid(
          Seq((Path \ "supervisorName") -> Seq(ValidationError("error.required.supervision.supervisor")),
            (Path \ "endingReason") -> Seq(ValidationError("error.required.supervision.reason")))
        )

        AnotherBody.formRule.validate(urlFormEncoded) must be(expected)

      }
    }
  }


  "Json read and writes" must {
    "successfully write No" in {
      val expected = Map("anotherBody" -> Seq("false"))
      AnotherBody.formWrites.writes(AnotherBodyNo) must be(expected)
    }

    "successfully write Yes" in {

      val expected = Map(
        "anotherBody" -> Seq("true"),
        "supervisorName" -> Seq("Name"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("2"),
        "startDate.year" -> Seq("1990"),
        "endDate.day" -> Seq("24"),
        "endDate.month" -> Seq("2"),
        "endDate.year" -> Seq("1998"),
        "endingReason" -> Seq("Reason")
      )

      val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
      val end = new LocalDate(1998, 2, 24) //scalastyle:off magic.number
      val input = AnotherBodyYes("Name", start, end, "Reason")

      AnotherBody.formWrites.writes(input) must be(expected)
    }
    "Serialise AnotherBodyNo as expected" in {
      Json.toJson(AnotherBodyNo) must be(Json.obj("anotherBody" -> false))
    }

    "Serialise AnotherBodyYes service as expected" in {

      val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
      val end = new LocalDate(1998, 2, 24)   //scalastyle:off magic.number
      val input = AnotherBodyYes("Name", start, end, "Reason")

      val expectedJson = Json.obj(
        "anotherBody" -> true,
        "supervisorName" -> "Name",
        "startDate" -> "1990-02-24",
        "endDate" -> "1998-02-24",
        "endingReason" -> "Reason"
      )

      Json.toJson(input) must be(expectedJson)
    }

    "Deserialise AnotherBodyNo as expected" in {
      val json = Json.obj("anotherBody" -> false)
      val expected = JsSuccess(AnotherBodyNo, JsPath)
      Json.fromJson[AnotherBody](json) must be (expected)
    }

    "Deserialise AnotherBodyYes as expected" in {

      val input = Json.obj(
        "anotherBody" -> true,
        "supervisorName" -> "Name",
        "startDate" -> "1990-02-24",
        "endDate" -> "1998-02-24",
        "endingReason" -> "Reason"
      )

      val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
      val end = new LocalDate(1998, 2, 24)   //scalastyle:off magic.number
      val expected = AnotherBodyYes("Name", start, end, "Reason")

      Json.fromJson[AnotherBody](input) must be (JsSuccess(expected, JsPath))
    }
    
    "fail when missing all data" in {
      Json.fromJson[AnotherBody](Json.obj()) must
        be(JsError((JsPath \ "anotherBody") -> play.api.data.validation.ValidationError("error.path.missing")))
    }
  }
}
