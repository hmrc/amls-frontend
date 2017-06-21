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

import jto.validation.Valid
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


    "write correct data from enum value" in {}

    "throw error on invalid data" in {}

    "throw error on empty data" in {}

  }

  "JSON validation" must {

    "validate given an enum value" in {}

    "write the correct value" in {}

    "throw error for invalid data" in {}

  }

}
