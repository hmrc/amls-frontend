/*
 * Copyright 2020 HM Revenue & Customs
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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class AnotherBodySpec extends PlaySpec with MockitoSugar {

  trait Fixture {

    val start = Some(SupervisionStart(new LocalDate(1990, 2, 24)))  //scalastyle:off magic.number
    val end = Some(SupervisionEnd(new LocalDate(1998, 2, 24)))//scalastyle:off magic.number
    val reason = Some(SupervisionEndReasons("Reason"))
  }

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given 'no' selected" in {
        val urlFormEncoded = Map("anotherBody" -> Seq("false"))
        val expected = Valid(AnotherBodyNo)
        AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
      }

      "given 'yes' selected with valid Name" in new Fixture {

        val urlFormEncoded = Map(
          "anotherBody" -> Seq("true"),
          "supervisorName" -> Seq("Name"),
          "startDate" -> Seq(""),
          "endDate" -> Seq(""),
          "endingReason" -> Seq("")
        )

        val expected = Valid(AnotherBodyYes("Name", None, None, None))

        AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
      }
    }



    "fail validation" when {
      "missing values when Yes selected" in {
        val urlFormEncoded = Map("anotherBody" -> Seq("true"))
        val expected = Invalid(
          Seq((Path \ "supervisorName") -> Seq(ValidationError("error.required")))
        )
        AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
      }

      "given invalid characters in endingReason and supervisorName" in {
        val urlFormEncoded = Map(
          "anotherBody" -> Seq("true"),
          "supervisorName" -> Seq("invalid {} <>"))

        val expected = Invalid(Seq((Path \ "supervisorName") -> Seq(ValidationError("err.text.validation"))))

        AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
      }

      "given only spaces in endingReason and supervisorName" in {
        val urlFormEncoded = Map(
          "anotherBody" -> Seq("true"),
          "supervisorName" -> Seq("  "))

        val expected = Invalid(Seq((Path \ "supervisorName") -> Seq(ValidationError("error.required.supervision.supervisor"))))

        AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
      }
    }
  }


  "Json read and writes" must {
    "successfully write No" in {
      val expected = Map("anotherBody" -> Seq("false"))
      AnotherBody.formWrites.writes(AnotherBodyNo) must be(expected)
    }

    "successfully write Yes" in  new Fixture {

      val expected = Map(
        "anotherBody" -> Seq("true"),
        "supervisorName" -> Seq("Name")
      )

      val input = AnotherBodyYes("Name", None, None, None)

      AnotherBody.formWrites.writes(input) must be(expected)
    }
    "Serialise AnotherBodyNo as expected" in {
      Json.toJson(AnotherBodyNo) must be(Json.obj("anotherBody" -> false))
    }

    "Serialise AnotherBodyYes service as expected" in new Fixture {

      val input = AnotherBodyYes("Name", start, end, reason)

      val expectedJson = Json.obj(
        "anotherBody" -> true,
        "supervisorName" -> "Name",
        "startDate" -> Json.obj("supervisionStartDate" -> "1990-02-24"),
        "endDate" -> Json.obj("supervisionEndDate" -> "1998-02-24"),
        "endingReason" -> Json.obj("supervisionEndingReason" -> "Reason")
      )

      Json.toJson(input) must be(expectedJson)
    }

    "Deserialise AnotherBodyNo as expected" in {
      val json = Json.obj("anotherBody" -> false)
      val expected = JsSuccess(AnotherBodyNo, JsPath)
      Json.fromJson[AnotherBody](json) must be (expected)
    }

    "Deserialise AnotherBodyYes as expected" in new Fixture {

      val input = Json.obj(
        "anotherBody" -> true,
        "supervisorName" -> "Name",
        "startDate" -> Json.obj("supervisionStartDate" -> "1990-02-24"),
        "endDate" -> Json.obj("supervisionEndDate" -> "1998-02-24"),
        "endingReason" -> Json.obj("supervisionEndingReason" -> "Reason")
      )

      val expected = AnotherBodyYes("Name", start, end, reason)

      Json.fromJson[AnotherBody](input) must be (JsSuccess(expected, JsPath))
    }
    
    "fail when missing all data" in {
      Json.fromJson[AnotherBody](Json.obj()) must
        be(JsError((JsPath \ "anotherBody") -> play.api.data.validation.ValidationError("error.path.missing")))
    }
  }

  "isComplete" must {
    "return true for complete AnotherBodyYes" in new Fixture {
      val completeAnotherBodyYes = AnotherBodyYes("Name", start, end, reason)
      completeAnotherBodyYes.isComplete() mustBe true
    }

    "return false for incomplete AnotherBodyYes" in new Fixture {
      val completeAnotherBodyYes = AnotherBodyYes("Name", start, end, reason)
      completeAnotherBodyYes.copy(startDate = None).isComplete() mustBe false
    }
  }
}
