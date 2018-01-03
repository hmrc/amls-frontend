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

package models.payments

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{ValidationError, Path}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json, JsPath, JsSuccess}

class TypeOfBankSpec extends PlaySpec {

  "Form validation" must {
    "pass validation" when {
      "typeOfBank is true" in {

        val model = Map(
          "typeOfBank" -> Seq("true")
        )

        TypeOfBank.formRule.validate(model) must be(Valid(TypeOfBank(true)))
      }

      "typeOfBank is false" in {

        val model = Map(
          "typeOfBank" -> Seq("false")
        )

        TypeOfBank.formRule.validate(model) must be(Valid(TypeOfBank(false)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        TypeOfBank.formRule.validate(Map.empty) must be(Invalid(Seq(
          Path \ "typeOfBank" -> Seq(ValidationError("payments.typeofbank.error"))
        )))
      }

      "given invalid data" in {
        TypeOfBank.formRule.validate(Map("typeOfBank" -> Seq("abc123"))) must be(Invalid(Seq(
          Path \ "typeOfBank" -> Seq(ValidationError("payments.typeofbank.error"))
        )))
      }
    }

    "successfully write the model" in {

      TypeOfBank.formWrites.writes(TypeOfBank(true)) mustBe Map(
        "typeOfBank" -> Seq("true")
      )
    }
  }

}