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

package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class VATRegisteredSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {
    "successfully validate" when {
      "given a 'false' value" in {

        VATRegistered.formRule.validate(Map("registeredForVAT" -> Seq("false"))) must
          be(Valid(VATRegisteredNo))
      }

      "given a 'true' value with a valid vrn number" in {

        val data = Map(
          "registeredForVAT" -> Seq("true"),
          "vrnNumber" -> Seq("123456789")
        )

        VATRegistered.formRule.validate(data) must
          be(Valid(VATRegisteredYes("123456789")))
      }
    }

    "fail validation" when {
      "given missing mandatory field represented by an empty Map" in {

        VATRegistered.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "registeredForVAT") -> Seq(ValidationError("error.required.atb.registered.for.vat"))
          )))
      }

      "given missing mandatory field represented by an empty string" in {

        val data = Map(
          "registeredForVAT" -> Seq("")
        )

        VATRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "registeredForVAT") -> Seq(ValidationError("error.required.atb.registered.for.vat"))
          )))
      }

      "given a 'true' value with a missing vrn number represented by an empty string" in {

        val data = Map(
          "registeredForVAT" -> Seq("true"),
          "vrnNumber" -> Seq("")
        )

        VATRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "vrnNumber") -> Seq(ValidationError("error.required.vat.number"))
          )))
      }

      "given a 'true' value with a missing vrn number represented by a missing field" in {

        val data = Map(
          "registeredForVAT" -> Seq("true")
        )

        VATRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "vrnNumber") -> Seq(ValidationError("error.required"))
          )))
      }

      "given a 'true' value with a missing vrn number represented by a sequence of whitespace" in {

        val data = Map(
          "registeredForVAT" -> Seq("true"),
          "vrnNumber" -> Seq("      \t")
        )

        VATRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "vrnNumber") -> Seq(ValidationError("error.invalid.vat.number"))
          )))
      }

      "given a 'true' value with a vrn number with more than 9 characters" in {

        val data = Map(
          "registeredForVAT" -> Seq("true"),
          "vrnNumber" -> Seq("1" * 10)
        )

        VATRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "vrnNumber") -> Seq(ValidationError("error.invalid.vat.number"))
          )))
      }

      "given a 'true' value with a vrn number with fewer than 9 characters" in {

        val data = Map(
          "registeredForVAT" -> Seq("true"),
          "vrnNumber" -> Seq("1" * 8)
        )

        VATRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "vrnNumber") -> Seq(ValidationError("error.invalid.vat.number"))
          )))
      }

      "given a 'true' value with a vrn number containing non-numeric characters" in {

        val data = Map(
          "registeredForVAT" -> Seq("true"),
          "vrnNumber" -> Seq("1ab2cd3ef")
        )

        VATRegistered.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "vrnNumber") -> Seq(ValidationError("error.invalid.vat.number"))
          )))
      }
    }


    "write correct data from enum value" in {

      VATRegistered.formWrites.writes(VATRegisteredNo) must
        be(Map("registeredForVAT" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      VATRegistered.formWrites.writes(VATRegisteredYes("12345678")) must
        be(Map("registeredForVAT" -> Seq("true"), "vrnNumber" -> Seq("12345678")))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[VATRegistered](Json.obj("registeredForVAT" -> false)) must
        be(JsSuccess(VATRegisteredNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("registeredForVAT" -> true, "vrnNumber" -> "12345678")

      Json.fromJson[VATRegistered](json) must
        be(JsSuccess(VATRegisteredYes("12345678"), JsPath \ "vrnNumber"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("registeredForVAT" -> true)

      Json.fromJson[VATRegistered](json) must
        be(JsError((JsPath \ "vrnNumber") -> play.api.data.validation.ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(VATRegisteredNo) must
        be(Json.obj("registeredForVAT" -> false))

      Json.toJson(VATRegisteredYes("12345678")) must
        be(Json.obj(
          "registeredForVAT" -> true,
          "vrnNumber" -> "12345678"
        ))
    }
  }

}
