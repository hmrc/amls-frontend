/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class WithdrawalReasonSpec extends PlaySpec with Matchers with MockitoSugar {

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
      Json.fromJson[WithdrawalReason](
        Json.obj("withdrawalReason" -> "Other, please specify", "specifyOtherReason" -> "reason")
      ) must
        be(JsSuccess(WithdrawalReason.Other("reason"), JsPath \ "specifyOtherReason"))
    }

    "write the correct value" when {
      "OutOfScope" in {
        Json.toJson(WithdrawalReason.OutOfScope.asInstanceOf[WithdrawalReason]) must be(
          Json.obj("withdrawalReason" -> "Out of scope")
        )
      }
      "NotTradingInOwnRight" in {
        Json.toJson(WithdrawalReason.NotTradingInOwnRight.asInstanceOf[WithdrawalReason]) must be(
          Json.obj("withdrawalReason" -> "Not trading in own right")
        )
      }
      "UnderAnotherSupervisor" in {
        Json.toJson(WithdrawalReason.UnderAnotherSupervisor.asInstanceOf[WithdrawalReason]) must be(
          Json.obj("withdrawalReason" -> "Under another supervisor")
        )
      }
      "Other" in {
        Json.toJson(WithdrawalReason.Other("reason").asInstanceOf[WithdrawalReason]) must be(
          Json.obj("withdrawalReason" -> "Other, please specify", "specifyOtherReason" -> "reason")
        )
      }
    }

    "throw error" when {
      "enum value is invalid" in {
        Json.fromJson[WithdrawalReason](Json.obj("withdrawalReason" -> "10")) must
          be(JsError(JsPath -> play.api.libs.json.JsonValidationError("error.invalid")))
      }
      "enum is missing" in {
        Json.fromJson[WithdrawalReason](Json.obj()) must
          be(JsError(JsPath \ "withdrawalReason" -> play.api.libs.json.JsonValidationError("error.path.missing")))
      }
      "other reason is missing" in {
        Json.fromJson[WithdrawalReason](Json.obj("withdrawalReason" -> "Other, please specify")) must
          be(JsError(JsPath \ "specifyOtherReason" -> play.api.libs.json.JsonValidationError("error.path.missing")))
      }
    }

  }

}
