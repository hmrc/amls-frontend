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
import play.api.libs.json.{JsPath, JsSuccess}

class TransactionsInNext12MonthsSpec extends PlaySpec {

  "TransactionsInNext12Months" should {

    "Form Validation" must {

      "Successfully read form data for option yes" in {

        val map = Map("txnAmount" -> Seq("12345678963"))

        TransactionsInNext12Months.formRule.validate(map) must be(Valid(TransactionsInNext12Months("12345678963")))
      }

      "fail validation on missing field" in {

        val map = Map("txnAmount" -> Seq(""))

        TransactionsInNext12Months.formRule.validate(map) must be(Invalid(
          Seq( Path \ "txnAmount" -> Seq(ValidationError("error.required.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field" in {

        val map = Map("txnAmount" -> Seq("asas"))
        TransactionsInNext12Months.formRule.validate(map) must be(Invalid(
          Seq( Path \ "txnAmount" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field when it exceeds the max length" in {

        val map = Map("txnAmount" -> Seq("123"*10))
        TransactionsInNext12Months.formRule.validate(map) must be(Invalid(
          Seq( Path \ "txnAmount" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months")))))
      }

      "fail validation on invalid field1" in {

        val map = Map("txnAmount" -> Seq("123456"))
        TransactionsInNext12Months.formRule.validate(map) must be(Valid(TransactionsInNext12Months("123456")))
      }


      "successfully write form data" in {

        TransactionsInNext12Months.formWrites.writes(TransactionsInNext12Months("12345678963")) must be(Map("txnAmount" -> Seq("12345678963")))
      }
    }

    "Json Validation" must {

      "Successfully read/write Json data" in {

        TransactionsInNext12Months.format.reads(TransactionsInNext12Months.format.writes(
          TransactionsInNext12Months("12345678963"))) must be(JsSuccess(TransactionsInNext12Months("12345678963"), JsPath \ "txnAmount"))

      }
    }
  }
}
