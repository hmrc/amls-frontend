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

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{ValidationError, Path}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json, JsPath, JsSuccess}

class SoleProprietorOfAnotherBusinessSpec extends PlaySpec {

  "Form validation" must {
    "pass validation" when {
      "soleProprietorOfAnotherBusiness is a boolean" in {

        val model = Map(
          "soleProprietorOfAnotherBusiness" -> Seq("true")
        )

        SoleProprietorOfAnotherBusiness.formRule.validate(model) must be(Valid(SoleProprietorOfAnotherBusiness(true)))
      }

      "soleProprietorOfAnotherBusiness is a boolean and is false" in {

        val model = Map(
          "soleProprietorOfAnotherBusiness" -> Seq("false")
        )

        SoleProprietorOfAnotherBusiness.formRule.validate(model) must be(Valid(SoleProprietorOfAnotherBusiness(false)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        SoleProprietorOfAnotherBusiness.formRule.validate(Map.empty) must be(Invalid(Seq(
          Path \ "soleProprietorOfAnotherBusiness" -> Seq(ValidationError("error.required.rp.sole_proprietor"))
        )))
      }

      "given invalid data" in {
        SoleProprietorOfAnotherBusiness.formRule.validate(Map("soleProprietorOfAnotherBusiness" -> Seq("abc123"))) must be(Invalid(Seq(
          Path \ "soleProprietorOfAnotherBusiness" -> Seq(ValidationError("error.required.rp.sole_proprietor"))
        )))
      }
    }

    "successfully write the model" in {

      SoleProprietorOfAnotherBusiness.formWrites.writes(SoleProprietorOfAnotherBusiness(true)) mustBe Map(
        "soleProprietorOfAnotherBusiness" -> Seq("true")
      )
    }
  }

  "Json validation" must {

    "Read and write successfully" in {

      SoleProprietorOfAnotherBusiness.format.reads(SoleProprietorOfAnotherBusiness.format.writes(SoleProprietorOfAnotherBusiness(true))) must be(
        JsSuccess(SoleProprietorOfAnotherBusiness(true), JsPath \ "soleProprietorOfAnotherBusiness"))
    }

    "write successfully" in {
      SoleProprietorOfAnotherBusiness.format.writes(SoleProprietorOfAnotherBusiness(true)) must be(Json.obj("soleProprietorOfAnotherBusiness" -> true))
    }
  }
}