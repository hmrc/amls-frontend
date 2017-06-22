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

package models.withdrawal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class WithdrawalReasonSpec extends PlaySpec with MustMatchers with MockitoSugar{

  "Form Validation" must {

    "validate" when {
      "given an enum value" in {

        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("01"))) must
          be(Valid(WithdrawalReason.OutOfScope))

        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("02"))) must
          be(Valid(WithdrawalReason.NotTradingInOwnRight))

        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("03"))) must
          be(Valid(WithdrawalReason.UnderAnotherSupervisor))

        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("04"))) must
          be(Valid(WithdrawalReason.JoinedAWRSGroup))

      }
      "given enum value for other and string for reason" in {

        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("05"), "specifyOtherReason" -> Seq("other"))) must
          be(Valid(WithdrawalReason.Other("other")))

      }
    }


    "write correct data" when {
      "from enum value" in {

        WithdrawalReason.formWrites.writes(WithdrawalReason.OutOfScope) must
          be(Map("withdrawalReason" -> Seq("01")))

        WithdrawalReason.formWrites.writes(WithdrawalReason.NotTradingInOwnRight) must
          be(Map("withdrawalReason" -> Seq("02")))

        WithdrawalReason.formWrites.writes(WithdrawalReason.UnderAnotherSupervisor) must
          be(Map("withdrawalReason" -> Seq("03")))

        WithdrawalReason.formWrites.writes(WithdrawalReason.JoinedAWRSGroup) must
          be(Map("withdrawalReason" -> Seq("04")))

      }
      "from enum value of Other and string of reason" in {
        WithdrawalReason.formWrites.writes(WithdrawalReason.Other("reason")) must
          be(Map("withdrawalReason" -> Seq("05"), "specifyOtherReason" -> Seq("reason")))
      }
    }

    "throw error" when {
      "invalid enum value" in {
        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("20"))) must
          be(Invalid(Seq((Path \ "withdrawalReason", Seq(ValidationError("error.invalid"))))))
      }
      "invalid characters other reason value" in {
        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("05"), "specifyOtherReason" -> Seq("{}"))) must
          be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("err.text.validation"))))))
      }
      "other reason value has too many characters" in {
        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("05"), "specifyOtherReason" -> Seq("a" * 256))) must
          be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.maxLength", 255))))))
      }

    }

    "throw error on empty" when {
      "non-selection of enum" in {
        WithdrawalReason.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "withdrawalReason", Seq(ValidationError("error.required.withdrawal.reason"))))))
      }
      "no other reason" which {
        "is an empty string" in {
          WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("05"), "specifyOtherReason" -> Seq(""))) must
            be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.required.withdrawal.reason.other"))))))
        }
        "a string of whitespace" in {
          WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("05"), "specifyOtherReason" -> Seq("   \t"))) must
            be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.required.withdrawal.reason.other"))))))
        }
        "a missing value" in {
          WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("05"), "specifyOtherReason" -> Seq.empty)) must
            be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.required"))))))
        }
      }

    }

  }

  "JSON validation" must {

    "validate given an enum value" in {}

    "write the correct value" in {}

    "throw error for invalid data" in {}

  }

}
