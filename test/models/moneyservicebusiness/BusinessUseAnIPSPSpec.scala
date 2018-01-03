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

package models.moneyservicebusiness

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import models.CharacterSets
import play.api.libs.json.{JsPath, JsSuccess}

import scala.collection.Seq

class BusinessUseAnIPSPSpec extends PlaySpec with CharacterSets {

  "BusinessUseAnIPSP" should {

    val formData = Map(
      "useAnIPSP" -> Seq("true"),
      "name" -> Seq("TEST"),
      "referenceNumber" -> Seq("09876543")
    )

    "FormValidation" must {

      "Successfully read form data option yes" when {

        "reference number is 8 digits" in {
          val ref = "98765432"
          val formData = Map(
            "useAnIPSP" -> Seq("true"),
            "name" -> Seq("TEST"),
            "referenceNumber" -> Seq(ref)
          )
          BusinessUseAnIPSP.formRule.validate(formData) must be(Valid(BusinessUseAnIPSPYes("TEST", ref)))
        }
        "reference number is 15 digits" in {
          val ref = "123456789123456"
          val formData = Map(
            "useAnIPSP" -> Seq("true"),
            "name" -> Seq("TEST"),
            "referenceNumber" -> Seq(ref)
          )
          BusinessUseAnIPSP.formRule.validate(formData) must be(Valid(BusinessUseAnIPSPYes("TEST", ref)))
        }
        "reference number is 15 alphanumeric with upper and lower" in {
          val ref = "AB3456789123xyz"
          val formData = Map(
            "useAnIPSP" -> Seq("true"),
            "name" -> Seq("TEST"),
            "referenceNumber" -> Seq(ref)
          )
          BusinessUseAnIPSP.formRule.validate(formData) must be(Valid(BusinessUseAnIPSPYes("TEST", ref)))
        }

      }

      "Successfully read form data option no" in {

        val map = Map("useAnIPSP" -> Seq("false"))
        BusinessUseAnIPSP.formRule.validate(map) must be(Valid(BusinessUseAnIPSPNo))
      }

      "Throw an error message" when {

        "missing mandatory field" in {
          BusinessUseAnIPSP.formRule.validate(Map.empty) must be(Invalid(Seq((Path \ "useAnIPSP", Seq(ValidationError("error.required.msb.ipsp"))))))
        }

        "missing mandatory field for option yes" in {
          val map = Map(
            "useAnIPSP" -> Seq("true"),
            "name" -> Seq(""),
            "referenceNumber" -> Seq("1234567891")
          )
          BusinessUseAnIPSP.formRule.validate(map) must be(Invalid(Seq((Path \ "name", Seq(ValidationError("error.required.msb.ipsp.name"))),
            (Path \ "referenceNumber", Seq(ValidationError("error.invalid.mlr.number"))))))
        }

        "name is too long" in {
          val map = Map(
            "useAnIPSP" -> Seq("true"),
            "name" -> Seq("abcd" * 100),
            "referenceNumber" -> Seq("1234567891")
          )
          BusinessUseAnIPSP.formRule.validate(map) must be(Invalid(Seq((Path \ "name", Seq(ValidationError("error.invalid.maxlength.140"))),
            (Path \ "referenceNumber", Seq(ValidationError("error.invalid.mlr.number"))))))
        }

        "reference is 8 alphanumerics" in {
          val ref = "alpha123"
          val formData = Map(
            "useAnIPSP" -> Seq("true"),
            "name" -> Seq("TEST"),
            "referenceNumber" -> Seq(ref)
          )
          BusinessUseAnIPSP.formRule.validate(formData) must be(Invalid(Seq(
            (Path \ "referenceNumber", Seq(ValidationError("error.invalid.mlr.number")))
          )))
        }

        "reference is neither 8 nor 15 in length" in {
          val ref = "9876765435432"
          val formData = Map(
            "useAnIPSP" -> Seq("true"),
            "name" -> Seq("TEST"),
            "referenceNumber" -> Seq(ref)
          )
          BusinessUseAnIPSP.formRule.validate(formData) must be(Invalid(Seq(
            (Path \ "referenceNumber", Seq(ValidationError("error.invalid.mlr.number")))
          )))
        }

        "reference contains invalid characters" in {
          val ref = symbols1.mkString("").take(8)
          val formData = Map(
            "useAnIPSP" -> Seq("true"),
            "name" -> Seq("TEST"),
            "referenceNumber" -> Seq(ref)
          )
          BusinessUseAnIPSP.formRule.validate(formData) must be(Invalid(Seq(
            (Path \ "referenceNumber", Seq(ValidationError("error.invalid.mlr.number")))
          )))
        }

      }
      "Successfully write form data" in {

        val obj = BusinessUseAnIPSPYes("TEST", "09876543")
        BusinessUseAnIPSP.formWrites.writes(obj) must be(formData)

      }

      "Successfully write form data for option no" in {

        val obj = BusinessUseAnIPSPNo
        BusinessUseAnIPSP.formWrites.writes(obj) must be(Map("useAnIPSP" -> Seq("false")))
      }
    }

    "JsonValidation" must {

      "Successfully read the Json value" in {
        val data = BusinessUseAnIPSPYes("TEST", "123456789123456")
        BusinessUseAnIPSP.jsonReads.reads(BusinessUseAnIPSP.jsonWrites.writes(data)) must be(JsSuccess(data, JsPath))

      }

      "Successfully read the Json value for option no" in {
        val data = BusinessUseAnIPSPNo
        BusinessUseAnIPSP.jsonReads.reads(BusinessUseAnIPSP.jsonWrites.writes(data)) must be(JsSuccess(data, JsPath))

      }
    }
  }
}
