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

package models.deregister

import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class DeregistrationReasonSpec extends PlaySpec with Matchers with MockitoSugar {

  "JSON validation" must {

    "validate given an enum value" when {
      "OutOfScope" in {
        Json.fromJson[DeregistrationReason](Json.obj("deregistrationReason" -> "Out of scope")) must
          be(JsSuccess(DeregistrationReason.OutOfScope, JsPath))
      }
      "NotTradingInOwnRight" in {
        Json.fromJson[DeregistrationReason](Json.obj("deregistrationReason" -> "Not trading in own right")) must
          be(JsSuccess(DeregistrationReason.NotTradingInOwnRight, JsPath))
      }
      "UnderAnotherSupervisor" in {
        Json.fromJson[DeregistrationReason](Json.obj("deregistrationReason" -> "Under another supervisor")) must
          be(JsSuccess(DeregistrationReason.UnderAnotherSupervisor, JsPath))
      }
      "ChangeOfLegalEntity" in {
        Json.fromJson[DeregistrationReason](Json.obj("deregistrationReason" -> "Change of Legal Entity")) must
          be(JsSuccess(DeregistrationReason.ChangeOfLegalEntity, JsPath))
      }
      "HVDPolicyOfNotAcceptingHighValueCashPayments" in {
        Json.fromJson[DeregistrationReason](
          Json.obj("deregistrationReason" -> "HVD - policy of not accepting high value cash payments")
        ) must
          be(JsSuccess(DeregistrationReason.HVDPolicyOfNotAcceptingHighValueCashPayments, JsPath))
      }
    }

    "validate given an enum value and string" in {
      Json.fromJson[DeregistrationReason](
        Json.obj("deregistrationReason" -> "Other, please specify", "specifyOtherReason" -> "reason")
      ) must
        be(JsSuccess(DeregistrationReason.Other("reason"), JsPath \ "specifyOtherReason"))
    }

    "write the correct value" when {
      "OutOfScope" in {
        Json.toJson(DeregistrationReason.OutOfScope.asInstanceOf[DeregistrationReason]) must be(
          Json.obj("deregistrationReason" -> "Out of scope")
        )
      }
      "NotTradingInOwnRight" in {
        Json.toJson(DeregistrationReason.NotTradingInOwnRight.asInstanceOf[DeregistrationReason]) must be(
          Json.obj("deregistrationReason" -> "Not trading in own right")
        )
      }
      "UnderAnotherSupervisor" in {
        Json.toJson(DeregistrationReason.UnderAnotherSupervisor.asInstanceOf[DeregistrationReason]) must be(
          Json.obj("deregistrationReason" -> "Under another supervisor")
        )
      }
      "ChangeOfLegalEntity" in {
        Json.toJson(DeregistrationReason.ChangeOfLegalEntity.asInstanceOf[DeregistrationReason]) must be(
          Json.obj("deregistrationReason" -> "Change of Legal Entity")
        )
      }
      "HVDPolicyOfNotAcceptingHighValueCashPayments" in {
        Json.toJson(
          DeregistrationReason.HVDPolicyOfNotAcceptingHighValueCashPayments.asInstanceOf[DeregistrationReason]
        ) must be(Json.obj("deregistrationReason" -> "HVD - policy of not accepting high value cash payments"))
      }
      "Other" in {
        Json.toJson(DeregistrationReason.Other("reason").asInstanceOf[DeregistrationReason]) must be(
          Json.obj("deregistrationReason" -> "Other, please specify", "specifyOtherReason" -> "reason")
        )
      }
    }

    "throw error" when {
      "enum value is invalid" in {
        Json.fromJson[DeregistrationReason](Json.obj("deregistrationReason" -> "10")) must
          be(JsError(JsPath -> play.api.libs.json.JsonValidationError("error.invalid")))
      }
      "enum is missing" in {
        Json.fromJson[DeregistrationReason](Json.obj()) must
          be(JsError(JsPath \ "deregistrationReason" -> play.api.libs.json.JsonValidationError("error.path.missing")))
      }
      "other reason is missing" in {
        Json.fromJson[DeregistrationReason](Json.obj("deregistrationReason" -> "Other, please specify")) must
          be(JsError(JsPath \ "specifyOtherReason" -> play.api.libs.json.JsonValidationError("error.path.missing")))
      }
    }

  }

}
