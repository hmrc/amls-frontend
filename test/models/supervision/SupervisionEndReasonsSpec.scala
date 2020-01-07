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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class SupervisionEndReasonsSpec extends PlaySpec with MockitoSugar {
  trait Fixture {

    val reason = "Reason"
  }

  "Form Rules and Writes" must {
    "given 'yes' selected with valid reason " in new Fixture {

      val urlFormEncoded = Map(
        "anotherBody" -> Seq("true"),
        "endingReason" -> Seq("Reason")
      )

      val expected = Valid(SupervisionEndReasons(reason))

      SupervisionEndReasons.formRule.validate(urlFormEncoded) must be(expected)
    }

    "fail validation" when {
      "given invalid characters in endingReason" in {

        val urlFormEncoded = Map(
          "anotherBody" -> Seq("true"),
          "endingReason" -> Seq("invalid {} <>"))

        val expected = Invalid(Seq((Path \ "endingReason") -> Seq(ValidationError("err.text.validation"))))

        SupervisionEndReasons.formRule.validate(urlFormEncoded) must be(expected)
      }

      "given only spaces in endingReason" in {
        val urlFormEncoded = Map(
          "anotherBody" -> Seq("true"),
          "endingReason" -> Seq("  "))

        val expected = Invalid(Seq((Path \ "endingReason") -> Seq(ValidationError("error.required.supervision.reason"))))

        SupervisionEndReasons.formRule.validate(urlFormEncoded) must be(expected)
      }
    }
  }

  "Json read and writes" must {
    "successfully write reason" in  new Fixture {

      val expected = Map(
        "anotherBody" -> Seq("true"),
        "endingReason" -> Seq("Reason")
      )

      val input = SupervisionEndReasons(reason)

      SupervisionEndReasons.formWrites.writes(input) must be(expected)
    }

    "Serialise SupervisionEndReasons as expected" in new Fixture {

      val input = SupervisionEndReasons(reason)

      val expectedJson = Json.obj("supervisionEndingReason" -> "Reason")

      Json.toJson(input) must be(expectedJson)
    }

    "Deserialise SupervisionEndReasons as expected" in new Fixture {

      val input = Json.obj(
        "anotherBody" -> true,
        "supervisionEndingReason" -> "Reason")

      val expected = SupervisionEndReasons(reason)

      Json.fromJson[SupervisionEndReasons](input) must be (JsSuccess(expected, JsPath \ "supervisionEndingReason"))
    }

    "fail when data is missing" in {
      Json.fromJson[SupervisionEndReasons](Json.obj()) must
        be(JsError((JsPath \ "supervisionEndingReason") -> play.api.data.validation.ValidationError("error.path.missing")))
    }
  }
}
