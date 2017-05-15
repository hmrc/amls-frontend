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

package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class SaRegisteredSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "pass validation" when {
      "successfully validate given a 'no' value" in {
        SaRegistered.formRule.validate(Map("saRegistered" -> Seq("false"))) must
          be(Valid(SaRegisteredNo))
      }

      "successfully validate given an `Yes` value with a valid utr" in {
        val data = Map(
          "saRegistered" -> Seq("true"),
          "utrNumber" -> Seq("0123456789")
        )

        SaRegistered.formRule.validate(data) must
          be(Valid(SaRegisteredYes("0123456789")))
      }
    }

    "fail validation" when {
      "fail to validate given saRegistered has no value" in {

        SaRegistered.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "saRegistered") -> Seq(ValidationError("error.required.sa.registration"))
          )))
      }

      "fail to validate given saRegistered is true and utrNumber has no value" in {

        val data = Map(
          "saRegistered" -> Seq("true")
        )

        SaRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "utrNumber") -> Seq(ValidationError("error.required"))
          )))
      }

      "utr contains an empty string" in {

        val data = Map(
          "saRegistered" -> Seq("true"),
          "utrNumber" -> Seq("")
        )

        SaRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "utrNumber") -> Seq(ValidationError("error.required.utr.number"))
          )))
      }

      "utr contains only whitespace" in {

        val data = Map(
          "saRegistered" -> Seq("true"),
          "utrNumber" -> Seq("   ")
        )

        SaRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "utrNumber") -> Seq(ValidationError("error.invalid.length.utr.number"))
          )))
      }

      "utr contains invalid characters" in {

        val data = Map(
          "saRegistered" -> Seq("true"),
          "utrNumber" -> Seq("12ab{}1230")
        )

        SaRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "utrNumber") -> Seq(ValidationError("error.invalid.length.utr.number"))
          )))
      }

      "utr contains too many numbers" in {

        val data = Map(
          "saRegistered" -> Seq("true"),
          "utrNumber" -> Seq("1" * 11)
        )

        SaRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "utrNumber") -> Seq(ValidationError("error.invalid.length.utr.number"))
          )))
      }
    }

    "write correct data from enum value" in {

      SaRegistered.formWrites.writes(SaRegisteredNo) must
        be(Map("saRegistered" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      SaRegistered.formWrites.writes(SaRegisteredYes("0123456789")) must
        be(Map("saRegistered" -> Seq("true"), "utrNumber" -> Seq("0123456789")))
    }

  }

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[SaRegistered](Json.obj("saRegistered" -> false)) must
        be(JsSuccess(SaRegisteredNo, JsPath ))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("saRegistered" -> true, "utrNumber" ->"0123456789")

      Json.fromJson[SaRegistered](json) must
        be(JsSuccess(SaRegisteredYes("0123456789"), JsPath  \ "utrNumber"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("saRegistered" -> true)

      Json.fromJson[SaRegistered](json) must
        be(JsError((JsPath  \ "utrNumber") -> play.api.data.validation.ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(SaRegisteredNo) must
        be(Json.obj("saRegistered" -> false))

      Json.toJson(SaRegisteredYes("0123456789")) must
        be(Json.obj(
          "saRegistered" -> true,
          "utrNumber" -> "0123456789"
        ))
    }
  }

}
