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

import models.payments.PaymentStatuses.Created
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import utils.AmlsSpec

import java.time.LocalDateTime

class PaymentSpec extends AmlsSpec with ScalaFutures {

  "Payment" must {
    "round trip through JSON formatting" in {

      val payment = Payment(
        "59b1204b2e000028005c0442",
        "XSML00000200738",
        "",
        "XH002610109496",
        Some("BACS Payment"),
        31500,
        Created,
        LocalDateTime.of(2017, 9, 7, 10, 32, 43, 526),
        Some(true)
      )

      val json = Json.toJson(payment)
      json.as[Payment] mustBe payment
    }
  }
}
