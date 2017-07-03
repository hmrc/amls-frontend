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

package models.deregister

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class DeregistrationReasonSpec extends PlaySpec with MustMatchers with MockitoSugar {

  "Form Validation" must {

    "validate" when {
      "given an enum value" in {

        DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("01"))) must
          be(Valid(DeregistrationReason.OutOfScope))

        DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("02"))) must
          be(Valid(DeregistrationReason.NotTradingInOwnRight))

        DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("03"))) must
          be(Valid(DeregistrationReason.UnderAnotherSupervisor))

        DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("04"))) must
          be(Valid(DeregistrationReason.ChangeOfLegalEntity))

        DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("05"))) must
          be(Valid(DeregistrationReason.HVDPolicyOfNotAcceptingHighValueCashPayments))

      }
      "given enum value for other and string for reason" in {
        DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("06"), "specifyOtherReason" -> Seq("other"))) must
          be(Valid(DeregistrationReason.Other("other")))
      }
    }

    "write correct data" when {
      "from enum value" in {

        DeregistrationReason.formWrites.writes(DeregistrationReason.OutOfScope) must
          be(Map("deregistrationReason" -> Seq("01")))

        DeregistrationReason.formWrites.writes(DeregistrationReason.NotTradingInOwnRight) must
          be(Map("deregistrationReason" -> Seq("02")))

        DeregistrationReason.formWrites.writes(DeregistrationReason.UnderAnotherSupervisor) must
          be(Map("deregistrationReason" -> Seq("03")))

        DeregistrationReason.formWrites.writes(DeregistrationReason.ChangeOfLegalEntity) must
          be(Map("deregistrationReason" -> Seq("04")))

        DeregistrationReason.formWrites.writes(DeregistrationReason.HVDPolicyOfNotAcceptingHighValueCashPayments) must
          be(Map("deregistrationReason" -> Seq("05")))

      }
      "from enum value of Other and string of reason" in {
        DeregistrationReason.formWrites.writes(DeregistrationReason.Other("reason")) must
          be(Map("deregistrationReason" -> Seq("06"), "specifyOtherReason" -> Seq("reason")))
      }
    }

    "throw error" when {
      "invalid enum value" in {
        DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("20"))) must
          be(Invalid(Seq((Path \ "deregistrationReason", Seq(ValidationError("error.invalid"))))))
      }
      "invalid characters other reason value" in {
        DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("06"), "specifyOtherReason" -> Seq("{}"))) must
          be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("err.text.validation"))))))
      }
      "other reason value has too many characters" in {
        DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("06"), "specifyOtherReason" -> Seq("a" * 41))) must
          be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.maxLength", 40))))))
      }
    }

    "throw error on empty" when {
      "non-selection of enum" in {
        DeregistrationReason.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "deregistrationReason", Seq(ValidationError("error.required.deregistration.reason"))))))
      }
      "no other reason" which {
        "is an empty string" in {
          DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("06"), "specifyOtherReason" -> Seq(""))) must
            be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.required.deregistration.reason.other"))))))
        }
        "a string of whitespace" in {
          DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("06"), "specifyOtherReason" -> Seq("   \t"))) must
            be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.required.deregistration.reason.other"))))))
        }
        "a missing value" in {
          DeregistrationReason.formRule.validate(Map("deregistrationReason" -> Seq("06"), "specifyOtherReason" -> Seq.empty)) must
            be(Invalid(Seq((Path \ "specifyOtherReason", Seq(ValidationError("error.required"))))))
        }
      }
    }

  }

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
        Json.fromJson[DeregistrationReason](Json.obj("deregistrationReason" -> "HVD - policy of not accepting high value cash payments")) must
          be(JsSuccess(DeregistrationReason.HVDPolicyOfNotAcceptingHighValueCashPayments, JsPath))
      }
    }

    "validate given an enum value and string" in {
      Json.fromJson[DeregistrationReason](Json.obj("deregistrationReason" -> "Other, please specify", "specifyOtherReason" -> "reason")) must
        be(JsSuccess(DeregistrationReason.Other("reason"), JsPath \ "specifyOtherReason"))
    }

    "write the correct value" when {
      "OutOfScope" in {
        Json.toJson(DeregistrationReason.OutOfScope) must be(Json.obj("deregistrationReason" -> "Out of scope"))
      }
      "NotTradingInOwnRight" in {
        Json.toJson(DeregistrationReason.NotTradingInOwnRight) must be(Json.obj("deregistrationReason" -> "Not trading in own right"))
      }
      "UnderAnotherSupervisor" in {
        Json.toJson(DeregistrationReason.UnderAnotherSupervisor) must be(Json.obj("deregistrationReason" -> "Under another supervisor"))
      }
      "ChangeOfLegalEntity" in {
        Json.toJson(DeregistrationReason.ChangeOfLegalEntity) must be(Json.obj("deregistrationReason" -> "Change of Legal Entity"))
      }
      "HVDPolicyOfNotAcceptingHighValueCashPayments" in {
        Json.toJson(DeregistrationReason.HVDPolicyOfNotAcceptingHighValueCashPayments) must be(Json.obj("deregistrationReason" -> "HVD - policy of not accepting high value cash payments"))
      }
      "Other" in {
        Json.toJson(DeregistrationReason.Other("reason")) must be(Json.obj("deregistrationReason" -> "Other, please specify", "specifyOtherReason" -> "reason"))
      }
    }

    "throw error" when {
      "enum value is invalid" in {
        Json.fromJson[DeregistrationReason](Json.obj("deregistrationReason" -> "10")) must
          be(JsError(JsPath -> play.api.data.validation.ValidationError("error.invalid")))
      }
      "enum is missing" in {
        Json.fromJson[DeregistrationReason](Json.obj()) must
          be(JsError(JsPath \ "deregistrationReason" -> play.api.data.validation.ValidationError("error.path.missing")))
      }
      "other reason is missing" in {
        Json.fromJson[DeregistrationReason](Json.obj("deregistrationReason" -> "Other, please specify")) must
          be(JsError(JsPath \ "specifyOtherReason" -> play.api.data.validation.ValidationError("error.path.missing")))
      }
    }

  }

}
