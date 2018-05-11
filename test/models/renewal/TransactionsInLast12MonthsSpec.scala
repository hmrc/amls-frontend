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

import cats.implicits._
import jto.validation.{Invalid, Path, Valid, ValidationError}
import play.api.libs.json.Json
import utils.AmlsSpec

class TransactionsInLast12MonthsSpec extends AmlsSpec {

  "The form serialiser returns the correct model" when {

    "given valid form data" in {
      val form = Map(
        "txnAmount" -> Seq("2000")
      )

      TransactionsInLast12Months.formReader.validate(form) mustBe Valid(TransactionsInLast12Months("2000"))
    }

    "given an empty string" in {
      val form = Map("txnAmount" -> Seq(""))
      TransactionsInLast12Months.formReader.validate(form) mustBe Invalid(Seq(Path \ "txnAmount" -> Seq(ValidationError("renewal.msb.transfers.value.invalid"))))
    }

    "given a string with no value" in {
      val form = Map("txnAmount" -> Seq(""))
      TransactionsInLast12Months.formReader.validate(form) mustBe Invalid(Seq(Path \ "txnAmount" -> Seq(ValidationError("renewal.msb.transfers.value.invalid"))))
    }

    "given an empty map" in {
      val form = Map.empty[String, Seq[String]]
      TransactionsInLast12Months.formReader.validate(form) mustBe Invalid(Seq(Path \ "txnAmount" -> Seq(ValidationError("error.required"))))
    }

    "given something that's not a number" in {
      val form = Map("txnAmount" -> Seq("not a number"))
      TransactionsInLast12Months.formReader.validate(form) mustBe Invalid(Seq(Path \ "txnAmount" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months"))))
    }

    "given a number over 11 in length" in {
      val form = Map("txnAmount" -> Seq("12345678900987654321"))
      TransactionsInLast12Months.formReader.validate(form) mustBe Invalid(Seq(Path \ "txnAmount" -> Seq(ValidationError("error.invalid.msb.transactions.in.12months"))))
    }

  }

  "The model serialiser returns the correct form" when {
    "given a valid model" in {
      val model = TransactionsInLast12Months("1575")
      TransactionsInLast12Months.formWriter.writes(model) mustBe Map("txnAmount" -> Seq("1575"))
    }
  }

  "The json serialiser" must {
    "round-trip through json serialisation" in {
      val model = TransactionsInLast12Months("1200")
      Json.fromJson[TransactionsInLast12Months](Json.toJson(model)).asOpt mustBe model.some
    }
  }

}
