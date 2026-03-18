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

package models.payments

import models.payments.PaymentStatuses._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class PaymentStatusResultSpec extends PlaySpec {

  "PaymentStatusResult" must {

    "serialise and deserialise correctly with Successful status" in {
      val model = PaymentStatusResult("XAML00000567890", "payment-123", Successful)
      Json.toJson(model).as[PaymentStatusResult] mustEqual model
    }

    "serialise and deserialise correctly with Created status" in {
      val model = PaymentStatusResult("XAML00000567890", "payment-123", Created)
      Json.toJson(model).as[PaymentStatusResult] mustEqual model
    }

    "serialise and deserialise correctly with Failed status" in {
      val model = PaymentStatusResult("XAML00000567890", "payment-123", Failed)
      Json.toJson(model).as[PaymentStatusResult] mustEqual model
    }

    "serialise and deserialise correctly with Cancelled status" in {
      val model = PaymentStatusResult("XAML00000567890", "payment-123", Cancelled)
      Json.toJson(model).as[PaymentStatusResult] mustEqual model
    }

    "serialise and deserialise correctly with Sent status" in {
      val model = PaymentStatusResult("XAML00000567890", "payment-123", Sent)
      Json.toJson(model).as[PaymentStatusResult] mustEqual model
    }
  }
}