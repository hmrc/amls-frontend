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

package models.businessmatching

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class ConfirmPostcodeSpec extends PlaySpec with MockitoSugar {

  "ConfirmPostcode" must {

    "Form Validation" must {

      "successfully validate" when {
        "given a correct numeric value with 8 digits" in {
          val data = Map("postCode" -> Seq("AA11AA"))
          val result = ConfirmPostcode.formReads.validate(data)
          result mustBe Valid(ConfirmPostcode("AA11AA"))
        }
      }

      "fail validation" when {
        "missing a mandatory field represented by an empty string" in {
          val result = ConfirmPostcode.formReads.validate(Map("postCode" -> Seq("")))
          result mustBe Invalid(Seq((Path \ "postCode") -> Seq(ValidationError("error.invalid.postcode"))))
        }
        "missing a mandatory field represented by an empty Map" in {
          val result = ConfirmPostcode.formReads.validate(Map.empty)
          result mustBe Invalid(Seq((Path \ "postCode") -> Seq(ValidationError("error.required"))))
        }
      }

      "write correct data from correct value" in {
        val result = ConfirmPostcode.formWrites.writes(ConfirmPostcode("AA11AA"))
        result must be(Map("postCode" -> Seq("AA11AA")))
      }
    }

    "Json validation" must {
      "READ the JSON successfully and return the domain Object" in {
        val postCode = ConfirmPostcode("AA11AA")
        val jsonConfirmPostcode = Json.obj("postCode" -> "AA11AA")
        val fromJson = Json.fromJson[ConfirmPostcode](jsonConfirmPostcode)
        fromJson must be(JsSuccess(postCode, JsPath \ "postCode"))
      }

      "read write json successfully" in {
        val postCode = ConfirmPostcode("AA11AA")
        ConfirmPostcode.format.reads(ConfirmPostcode.format.writes(postCode)) must
          be(JsSuccess(postCode, JsPath \ "postCode"))
      }

    }
  }
}