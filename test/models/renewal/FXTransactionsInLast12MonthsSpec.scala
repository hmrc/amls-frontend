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

package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess}

class FXTransactionsInLast12MonthsSpec extends PlaySpec {

  "FXTransactionIn" should {

    "Form Validation" must {

      "Successfully read form data for option yes" in {
        val map = Map("fxTransaction" -> Seq("12345678963"))

        FXTransactionsInLast12Months.formRule.validate(map) must be(Valid(FXTransactionsInLast12Months("12345678963")))
      }

      "fail validation on missing field" in {
        val map = Map("fxTransaction" -> Seq(""))

        FXTransactionsInLast12Months.formRule.validate(map) must be(Invalid(
          Seq(Path \ "fxTransaction" -> Seq(ValidationError("error.required.renewal.fx.transactions.in.12months")))))
      }

      "fail validation on invalid field" in {
        val map = Map("fxTransaction" -> Seq("asas"))

        FXTransactionsInLast12Months.formRule.validate(map) must be(Invalid(
          Seq(Path \ "fxTransaction" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field when it exceeds the max length" in {
        val map = Map("fxTransaction" -> Seq("123" * 10))

        FXTransactionsInLast12Months.formRule.validate(map) must be(Invalid(
          Seq(Path \ "fxTransaction" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field1" in {
        val map = Map("fxTransaction" -> Seq("123456"))

        FXTransactionsInLast12Months.formRule.validate(map) must be(Valid(FXTransactionsInLast12Months("123456")))
      }

      "successfully write form data" in {
        FXTransactionsInLast12Months.formWrites.writes(FXTransactionsInLast12Months("12345678963")) must be(Map("fxTransaction" -> Seq("12345678963")))
      }
    }

    "Json Validation" must {

      "Successfully read/write Json data" in {
        FXTransactionsInLast12Months.format.reads(FXTransactionsInLast12Months.format.writes(
          FXTransactionsInLast12Months("12345678963"))) must be(JsSuccess(FXTransactionsInLast12Months("12345678963"), JsPath \ "fxTransaction"))
      }
    }
  }
}
