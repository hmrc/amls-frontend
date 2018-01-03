/*
 * Copyright 2018 HM Revenue & Customs
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

class TrainingSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "pass validation" when {
      "training is false" in {
        Training.formRule.validate(Map("training" -> Seq("false"))) must
          be(Valid(TrainingNo))
      }

      "given an `Yes` value with a valid information string" in {
        val data = Map(
          "training" -> Seq("true"),
          "information" -> Seq("test")
        )

        Training.formRule.validate(data) must be(Valid(TrainingYes("test")))
      }
    }

    "fail validation" when {
      "given an `Yes` with no value" in {

        val data = Map(
          "training" -> Seq("true")
        )

        Training.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "information") -> Seq(ValidationError("error.required"))
          )))
      }
      "given an `Yes` with an empty string" in {

        val data = Map(
          "training" -> Seq("true"),
          "information" -> Seq("")
        )

        Training.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "information") -> Seq(ValidationError("error.required.rp.training.information"))
          )))
      }
      "given an `Yes` with a sequence of whitespace" in {

        val data = Map(
          "training" -> Seq("true"),
          "information" -> Seq("   ")
        )

        Training.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "information") -> Seq(ValidationError("error.required.rp.training.information"))
          )))
      }
      "given an `Yes` with too many characters" in {

        val data = Map(
          "training" -> Seq("true"),
          "information" -> Seq("a" * 256)
        )

        Training.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "information") -> Seq(ValidationError("error.invalid.maxlength.255"))
          )))
      }
      "given an `Yes` with invalid characters" in {

        val data = Map(
          "training" -> Seq("true"),
          "information" -> Seq("AAA{}AAA")
        )

        Training.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "information") -> Seq(ValidationError("err.text.validation"))
          )))
      }
    }

    "write correct data from enum value" in {

      Training.formWrites.writes(TrainingNo) must be(Map("training" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      Training.formWrites.writes(TrainingYes("0123456789")) must
        be(Map("training" -> Seq("true"), "information" -> Seq("0123456789")))
    }

  }

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[Training](Json.obj("training" -> false)) must
        be(JsSuccess(TrainingNo, JsPath ))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("training" -> true, "information" -> "0123456789")

      Json.fromJson[Training](json) must
        be(JsSuccess(TrainingYes("0123456789"), JsPath  \ "information"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("training" -> true)

      Json.fromJson[Training](json) must
        be(JsError((JsPath  \ "information") -> play.api.data.validation.ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(TrainingNo) must be(Json.obj("training" -> false))

      Json.toJson(TrainingYes("0123456789")) must
        be(Json.obj(
          "training" -> true,
          "information" -> "0123456789"
        ))
    }
  }

}
