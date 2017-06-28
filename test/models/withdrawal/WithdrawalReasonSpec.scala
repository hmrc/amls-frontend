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
import play.api.libs.json.{JsString, _}

class WithdrawalReasonSpec extends PlaySpec with MustMatchers with MockitoSugar {

  "Form Validation" must {

    "validate" when {
      "given an enum value" in {

        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("01"))) must
          be(Valid(WithdrawalReason.OutOfScope))

        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("02"))) must
          be(Valid(WithdrawalReason.NotTradingInOwnRight))

        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("03"))) must
          be(Valid(WithdrawalReason.UnderAnotherSupervisor))

      }
      "given enum value for other and string for reason" in {
        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("04"), "specifyOtherReason" -> Seq("other"))) must
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

      }
      "from enum value of Other and string of reason" in {
        WithdrawalReason.formWrites.writes(WithdrawalReason.Other("reason")) must
          be(Map("withdrawalReason" -> Seq("04"), "specifyOtherReason" -> Seq("reason")))
      }
    }

    "throw error" when {
      "invalid enum value" in {
        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("20"))) must
          be(Invalid(Seq((Path \ "withdrawalReason", Seq(ValidationError("error.invalid"))))))
      }
      "invalid characters other reason value" in {
        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("04"), "specifyOtherReason" -> Seq("{}"))) must
          be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("err.text.validation"))))))
      }
      "other reason value has too many characters" in {
        WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("04"), "specifyOtherReason" -> Seq("a" * 256))) must
          be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.maxLength", 40))))))
      }
    }

    "throw error on empty" when {
      "non-selection of enum" in {
        WithdrawalReason.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "withdrawalReason", Seq(ValidationError("error.required.withdrawal.reason"))))))
      }
      "no other reason" which {
        "is an empty string" in {
          WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("04"), "specifyOtherReason" -> Seq(""))) must
            be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.required.withdrawal.reason.other"))))))
        }
        "a string of whitespace" in {
          WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("04"), "specifyOtherReason" -> Seq("   \t"))) must
            be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.required.withdrawal.reason.other"))))))
        }
        "a missing value" in {
          WithdrawalReason.formRule.validate(Map("withdrawalReason" -> Seq("04"), "specifyOtherReason" -> Seq.empty)) must
            be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.required"))))))
        }
      }
    }

  }

  "JSON validation" must {

    "validate given an enum value" when {
      "OutOfScope" in {
        Json.fromJson[WithdrawalReason](Json.obj("withdrawalReason" -> "Out of scope")) must
          be(JsSuccess(WithdrawalReason.OutOfScope, JsPath))
      }
      "NotTradingInOwnRight" in {
        Json.fromJson[WithdrawalReason](Json.obj("withdrawalReason" -> "Not trading in own right")) must
          be(JsSuccess(WithdrawalReason.NotTradingInOwnRight, JsPath))
      }
      "UnderAnotherSupervisor" in {
        Json.fromJson[WithdrawalReason](Json.obj("withdrawalReason" -> "Under another supervisor")) must
          be(JsSuccess(WithdrawalReason.UnderAnotherSupervisor, JsPath))
      }
    }

    "validate given an enum value and string" in {
      Json.fromJson[WithdrawalReason](Json.obj("withdrawalReason" -> "Other, please specify", "specifyOtherReason" -> "reason")) must
        be(JsSuccess(WithdrawalReason.Other("reason"), JsPath \ "specifyOtherReason"))
    }

    "write the correct value" when {
      "OutOfScope" in {
        Json.toJson(WithdrawalReason.OutOfScope) must be(Json.obj("withdrawalReason" -> "Out of scope"))
      }
      "NotTradingInOwnRight" in {
        Json.toJson(WithdrawalReason.NotTradingInOwnRight) must be(Json.obj("withdrawalReason" -> "Not trading in own right"))
      }
      "UnderAnotherSupervisor" in {
        Json.toJson(WithdrawalReason.UnderAnotherSupervisor) must be(Json.obj("withdrawalReason" -> "Under another supervisor"))
      }
      "Other" in {
        Json.toJson(WithdrawalReason.Other("reason")) must be(Json.obj("withdrawalReason" -> "Other, please specify", "specifyOtherReason" -> "reason"))
      }
    }

    "throw error" when {
      "enum value is invalid" in {
        Json.fromJson[WithdrawalReason](Json.obj("withdrawalReason" -> "10")) must
          be(JsError(JsPath -> play.api.data.validation.ValidationError("error.invalid")))
      }
      "enum is missing" in {
        Json.fromJson[WithdrawalReason](Json.obj()) must
          be(JsError(JsPath \ "withdrawalReason" -> play.api.data.validation.ValidationError("error.path.missing")))
      }
      "other reason is missing" in {
        Json.fromJson[WithdrawalReason](Json.obj("withdrawalReason" -> "Other, please specify")) must
          be(JsError(JsPath \ "specifyOtherReason" -> play.api.data.validation.ValidationError("error.path.missing")))
      }
    }

  }

}
