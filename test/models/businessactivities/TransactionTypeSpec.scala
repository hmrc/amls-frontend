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

package models.businessactivities

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec

class TransactionTypeSpec extends PlaySpec with MustMatchers {

  "TransactionType" must {
    "pass validation" when {
      "yes is selected and a few check boxes are selected" in {
        val model = Map(
          "transactions[]" -> Seq("01", "02", "03"),
          "name" -> Seq("test")
        )

        TransactionType.formRule.validate(model) must
          be(Valid(Set(Paper, DigitalSpreadsheet, DigitalSoftware("test"))))
      }
    }

    "fail validation" when {
      "software is selected but software name is empty, represented by an empty string" in {
        val model = Map(
          "transactions[]" -> Seq("01", "02", "03"),
          "name" -> Seq("")
        )

        TransactionType.formRule.validate(model) must
          be(Invalid(List((Path \ "name", Seq(ValidationError("error.required.ba.software.package.name"))))))
      }

      "software name exceeds max length" in {
        val model = Map(
          "transactions[]" -> Seq("01", "02", "03"),
          "name" -> Seq("a" * 41)
        )

        TransactionType.formRule.validate(model) must
          be(Invalid(List((Path \ "name", Seq(ValidationError("error.invalid.maxlength.40"))))))
      }

      "software name contains invalid characters" in {
        val model = Map(
          "isRecorded" -> Seq("true"),
          "transactions[]" -> Seq("01", "02", "03"),
          "name" -> Seq("abc{}abc")
        )

        TransactionType.formRule.validate(model) must
          be(Invalid(List((Path \ "name", Seq(ValidationError("err.text.validation"))))))
      }

      "no check boxes are selected in transactions" in {
        val model = Map(
          "transactions[]" -> Seq()
        )

        TransactionType.formRule.validate(model) must
          be(Invalid(List((Path \ "transactions", Seq(ValidationError("error.required.ba.atleast.one.transaction.record"))))))
      }

      "given an empty Map" in {
        TransactionType.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "transactions") -> Seq(ValidationError("error.required.ba.atleast.one.transaction.record")))))
      }

      "given invalid enum value in transactions" in {
        val model = Map(
          "transactions[]" -> Seq("01, 10")
        )

        TransactionType.formRule.validate(model) must
          be(Invalid(Seq((Path \ "transactions") -> Seq(ValidationError("error.invalid")))))
      }
    }
  }
}
