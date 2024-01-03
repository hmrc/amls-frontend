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

import models.hvd.{PaymentMethods => HvdPaymentMethods}
import play.api.libs.json.Json
import utils.AmlsSpec

class CashPaymentsSpec extends AmlsSpec {

  val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))

  "CashPayments" must {
    "roundtrip through json for true" in {
      val data = CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(paymentMethods)))
      Json.toJson(data).as[CashPayments] mustBe data
    }

    "roundtrip through json for false" in {
      val data = CashPayments(CashPaymentsCustomerNotMet(false), None)
      Json.toJson(data).as[CashPayments] mustBe data
    }
  }

  "CashPayments convert" must {
    "convert renewal cash payment data to hvd payment methods" in {
      val renewalCp = CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(paymentMethods)))

      CashPayments.convert(renewalCp) mustBe Some(HvdPaymentMethods(courier = true, direct = true, Some("foo")))
    }
  }
}
