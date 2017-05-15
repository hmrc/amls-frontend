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

package models.tcsp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ServicesOfAnotherTCSPSpec extends PlaySpec with MockitoSugar {

  "services of another Tcsp" must {

    "pass validation" must {

      "successfully validate if no option selected" in {
        ServicesOfAnotherTCSP.formRule.validate(Map("servicesOfAnotherTCSP" -> Seq("false"))) must
          be(Valid(ServicesOfAnotherTCSPNo))
      }

      "successfully validate if `Yes` option selected and mandatory mlr number given" in {

        val data = Map(
          "servicesOfAnotherTCSP" -> Seq("true"),
          "mlrRefNumber" -> Seq("12345678")
        )

        ServicesOfAnotherTCSP.formRule.validate(data) must
          be(Valid(ServicesOfAnotherTCSPYes("12345678")))
      }

      "successfully validate if `Yes` option selected and mandatory mlr number given with alphanumeric value" in {

        val data = Map(
          "servicesOfAnotherTCSP" -> Seq("true"),
          "mlrRefNumber" -> Seq("i9w9834ubkid89n")
        )

        ServicesOfAnotherTCSP.formRule.validate(data) must be {
          Valid(ServicesOfAnotherTCSPYes("i9w9834ubkid89n"))
        }

      }
    }

    "fail validation" when {

      "fail when no option selected" in {
        ServicesOfAnotherTCSP.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "servicesOfAnotherTCSP") -> Seq(ValidationError("error.required.tcsp.services.another.tcsp"))
          )))

      }

      "fail to validate when `Yes` option selected with no value for the mlr number" in {

        val data = Map(
          "servicesOfAnotherTCSP" -> Seq("true"),
          "mlrRefNumber" -> Seq("")
        )

        ServicesOfAnotherTCSP.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "mlrRefNumber") -> Seq(ValidationError("error.invalid.mlr.number"))
          )))
      }


      "fail to when `Yes` option selected with invalid value for the mlr number" in {

        val data = Map(
          "servicesOfAnotherTCSP" -> Seq("true"),
          "mlrRefNumber" -> Seq("123qed")
        )

        ServicesOfAnotherTCSP.formRule.validate(data) must be(
          Invalid(Seq((Path \ "mlrRefNumber") -> Seq(ValidationError("error.invalid.mlr.number"))
          )))
      }
    }

    "form validation" when {

      "write correct data from enum value" in {

        ServicesOfAnotherTCSP.formWrites.writes(ServicesOfAnotherTCSPNo) must
          be(Map("servicesOfAnotherTCSP" -> Seq("false")))

      }

      "write correct data from `yes` value" in {

        ServicesOfAnotherTCSP.formWrites.writes(ServicesOfAnotherTCSPYes("12345678")) must
          be(Map("servicesOfAnotherTCSP" -> Seq("true"), "mlrRefNumber" -> Seq("12345678")))

      }
    }

    "JSON validation" when {
      import play.api.data.validation.ValidationError

      "successfully validate given an enum value" in {

        Json.fromJson[ServicesOfAnotherTCSP](Json.obj("servicesOfAnotherTCSP" -> false)) must
          be(JsSuccess(ServicesOfAnotherTCSPNo, JsPath))
      }

      "successfully validate given an `Yes` value" in {

        Json.fromJson[ServicesOfAnotherTCSP](Json.obj("servicesOfAnotherTCSP" -> true, "mlrRefNumber" -> "12345678")) must
          be(JsSuccess(ServicesOfAnotherTCSPYes("12345678"), JsPath \ "mlrRefNumber"))
      }

      "fail to validate when given an empty `Yes` value" in {

        val json = Json.obj("servicesOfAnotherTCSP" -> true)

        Json.fromJson[ServicesOfAnotherTCSP](json) must
          be(JsError((JsPath \ "mlrRefNumber") -> ValidationError("error.path.missing")))
      }

      "write the correct value" in {

        Json.toJson(ServicesOfAnotherTCSPNo) must
          be(Json.obj("servicesOfAnotherTCSP" -> false))

        Json.toJson(ServicesOfAnotherTCSPYes("12345678")) must
          be(Json.obj(
            "servicesOfAnotherTCSP" -> true,
            "mlrRefNumber" -> "12345678"
          ))
      }

    }


  }

}
