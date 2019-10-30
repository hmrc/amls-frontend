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

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class PreviouslyRegisteredSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate" when {
      "successfully validate given a 'no' value" in {

        PreviouslyRegistered.formRule.validate(Map("previouslyRegistered" -> Seq("false"))) must
          be(Valid(PreviouslyRegisteredNo))
      }

      "successfully validate given an `Yes` value with pre-existing MLR reg number" in {

        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("1" * 8)
        )

        PreviouslyRegistered.formRule.validate(data) must
          be(Valid(PreviouslyRegisteredYes(None)))
      }

      "successfully validate given an `Yes` value with no MLR reg number" in {

        val data = Map(
          "previouslyRegistered" -> Seq("true")
        )

        PreviouslyRegistered.formRule.validate(data) must
          be(Valid(PreviouslyRegisteredYes(None)))
      }

      "successfully validate given an `Yes` value with an empty MLR reg number" in {

        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("")
        )

        PreviouslyRegistered.formRule.validate(data) must
          be(Valid(PreviouslyRegisteredYes(None)))
      }
    }

    "fail validation" when {

      "given a 'yes' value with more than 8 characters" in {
        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("1" * 9)
        )

        be(Invalid(Seq(
          (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.invalid.mlr.number"))
        )))
      }
      "given a 'yes' value with less than 8 characters" in {
        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("1" * 5)
        )

        be(Invalid(Seq(
          (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.invalid.mlr.number"))
        )))
      }
      "given a 'yes' value with between 9 and 14 characters" in {
        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("1" * 11)
        )

        be(Invalid(Seq(
          (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.invalid.mlr.number"))
        )))
      }
      "given a 'yes' value non-numeric characters" in {
        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("1ghy7cnj&")
        )

        be(Invalid(Seq(
          (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.invalid.mlr.number"))
        )))
      }

      "given an empty map" in {

        PreviouslyRegistered.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "previouslyRegistered") -> Seq(ValidationError("error.required.atb.previously.registered"))
          )))
      }
    }

    "write correct data from enum value" in {

      PreviouslyRegistered.formWrites.writes(PreviouslyRegisteredNo) must
        be(Map("previouslyRegistered" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      PreviouslyRegistered.formWrites.writes(PreviouslyRegisteredYes(Some("12345678"))) must
        be(Map("previouslyRegistered" -> Seq("true"), "prevMLRRegNo" -> Seq("12345678")))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[PreviouslyRegistered](Json.obj("previouslyRegistered" -> false)) must
        be(JsSuccess(PreviouslyRegisteredNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("previouslyRegistered" -> true, "prevMLRRegNo" -> "12345678")

      Json.fromJson[PreviouslyRegistered](json) must
        be(JsSuccess(PreviouslyRegisteredYes(Some("12345678")), JsPath \ "prevMLRRegNo"))
    }

    "successfully validate validate when given an empty `Yes` value" in {

      val json = Json.obj("previouslyRegistered" -> true)

      Json.fromJson[PreviouslyRegistered](json) must
        be(JsSuccess(PreviouslyRegisteredYes(None)))
    }

    "write the correct value" in {

      Json.toJson(PreviouslyRegisteredNo) must
        be(Json.obj("previouslyRegistered" -> false))

      Json.toJson(PreviouslyRegisteredYes(Some("12345678"))) must
        be(Json.obj(
          "previouslyRegistered" -> true,
          "prevMLRRegNo" -> "12345678"
        ))
    }
  }
}
