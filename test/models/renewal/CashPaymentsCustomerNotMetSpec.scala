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

package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import utils.AmlsSpec

class CashPaymentsCustomerNotMetSpec extends AmlsSpec {

  "CashPaymentsCustomerNotMet" must {
    "roundtrip through form" in {
      val data = CashPaymentsCustomerNotMet(true)
      CashPaymentsCustomerNotMet.formRule.validate(CashPaymentsCustomerNotMet.formWrites.writes(data)) mustEqual Valid(data)
    }

    "fail to validate when no choice is made for question" in {
      val data = Map.empty[String, Seq[String]]
      CashPaymentsCustomerNotMet.formRule.validate(data)
        .mustEqual(Invalid(Seq((Path \ "receiveCashPayments") -> Seq(ValidationError("error.required.renewal.hvd.receive.cash.payments")))))
    }
  }
}
