/*
 * Copyright 2019 HM Revenue & Customs
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
import utils.AmlsSpec

class HowCashPaymentsReceivedSpec extends AmlsSpec {

  val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))

  "HowCashPaymentsReceived" must {
    "roundtrip through form" in {
      val data = HowCashPaymentsReceived(paymentMethods)
      HowCashPaymentsReceived.formRule.validate(HowCashPaymentsReceived.formWrites.writes(data)) mustEqual Valid(data)
    }

    "fail to validate when no method is selected" in {
      val data = Map.empty[String, Seq[String]]
      HowCashPaymentsReceived.formRule.validate(data)
        .mustEqual(Invalid(Seq((Path \ "cashPaymentMethods") -> Seq(ValidationError("error.required.renewal.hvd.choose.option")))))
    }

    "fail to validate when no text is entered in the details field" in {
      val data = Map("cashPaymentMethods.other" -> Seq("true"))

      HowCashPaymentsReceived.formRule.validate(data)
        .mustEqual(Invalid(Seq((Path \ "cashPaymentMethods" \ "details") -> Seq(ValidationError("error.required")))))
    }

    "fail to validate when more than 255 characters are entered in the details field" in {
      val data = Map("cashPaymentMethods.other" -> Seq("true"),
        "cashPaymentMethods.details" -> Seq("a" * 260))

      HowCashPaymentsReceived.formRule.validate(data)
        .mustEqual(Invalid(Seq((Path \ "cashPaymentMethods" \ "details") -> Seq(ValidationError("error.invalid.maxlength.255")))))
    }
  }
}
