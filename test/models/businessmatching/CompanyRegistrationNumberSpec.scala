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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class CompanyRegistrationNumberSpec extends PlaySpec with MockitoSugar {

  "CompanyRegistrationNumber" must {

    "Form Validation" must {

      "successfully validate" when {
        "given a correct numeric value with 8 digits" in {
          val data = Map("companyRegistrationNumber" -> Seq("12345678"))
          val result = CompanyRegistrationNumber.formReads.validate(data)
          result mustBe Valid(CompanyRegistrationNumber("12345678"))
        }


        "given a correct upper case alphanumeric value with 8 digits" in {
          val data = Map("companyRegistrationNumber" -> Seq("AB78JC12"))
          val result = CompanyRegistrationNumber.formReads.validate(data)
          result mustBe Valid(CompanyRegistrationNumber("AB78JC12"))
        }
      }

      "fail validation" when {
        "missing a mandatory field represented by an empty string" in {
          val result = CompanyRegistrationNumber.formReads.validate(Map("companyRegistrationNumber" -> Seq("")))
          result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.required.bm.registration.number"))))
        }
        "missing a mandatory field represented by an empty Map" in {
          val result = CompanyRegistrationNumber.formReads.validate(Map.empty)
          result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.required"))))
        }

        "given an alphanumeric value with 8 digits containing lower case letters" in {
          val data = Map("companyRegistrationNumber" -> Seq("ab765bhd"))
          val result = CompanyRegistrationNumber.formReads.validate(data)
          result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.invalid.bm.registration.number"))))
        }
        "given a value with length greater than 8" in {
          val data = Map("companyRegistrationNumber" -> Seq("1" * 9))
          val result = CompanyRegistrationNumber.formReads.validate(data)
          result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.invalid.bm.registration.number"))))
        }

        "given a value with length less than 8" in {
          val data = Map("companyRegistrationNumber" -> Seq("1" * 7))
          val result = CompanyRegistrationNumber.formReads.validate(data)
          result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.invalid.bm.registration.number"))))
        }

        "given a value containing non-alphanumeric characters" in {
          val data = Map("companyRegistrationNumber" -> Seq("1234567!"))
          val result = CompanyRegistrationNumber.formReads.validate(data)
          result mustBe Invalid(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.invalid.bm.registration.number"))))
        }
      }

      "write correct data from correct value" in {
        val result = CompanyRegistrationNumber.formWrites.writes(CompanyRegistrationNumber("12345678"))
        result must be(Map("companyRegistrationNumber" -> Seq("12345678")))
      }

    }

    "Json validation" must {

      "READ the JSON successfully and return the domain Object" in {
        val companyRegistrationNumber = CompanyRegistrationNumber("12345678")
        val jsonCompanyRegistrationNumber = Json.obj("companyRegistrationNumber" -> "12345678")
        val fromJson = Json.fromJson[CompanyRegistrationNumber](jsonCompanyRegistrationNumber)
        fromJson must be(JsSuccess(companyRegistrationNumber, JsPath \ "companyRegistrationNumber"))
      }

      "validate model with valid numeric registration number" in {
        val model = Map("companyRegistrationNumber" -> Seq("12345678"))
        CompanyRegistrationNumber.formReads.validate(model) must
          be(Valid(CompanyRegistrationNumber("12345678")))
      }

      "validate model with valid upper case registration number" in {
        val model = Map("companyRegistrationNumber" -> Seq("ABCDEFGH"))
        CompanyRegistrationNumber.formReads.validate(model) must
          be(Valid(CompanyRegistrationNumber("ABCDEFGH")))
      }

      "validate model with valid lower case registration number" in {
        val model = Map("companyRegistrationNumber" -> Seq("ABCDEFGH"))
        CompanyRegistrationNumber.formReads.validate(model) must
          be(Valid(CompanyRegistrationNumber("ABCDEFGH")))
      }

      "fail to validate when given data with length greater than 8" in {
        val model = Map("companyRegistrationNumber" -> Seq("1234567890"))
        CompanyRegistrationNumber.formReads.validate(model) mustBe Invalid(Seq(
          (Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.invalid.bm.registration.number"))))
      }

      "fail to validate when given data with length less than 8" in {
        val model = Map("companyRegistrationNumber" -> Seq("123"))
        CompanyRegistrationNumber.formReads.validate(model) mustBe Invalid(Seq(
          (Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.invalid.bm.registration.number"))))
      }

      "fail to validate when given data with non-alphanumeric characters" in {
        val model = Map("companyRegistrationNumber" -> Seq("1234567!"))
        CompanyRegistrationNumber.formReads.validate(model) mustBe Invalid(Seq(
          (Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.invalid.bm.registration.number"))))
      }
    }
  }
}