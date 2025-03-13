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

package models.hvd

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class ReceiveCashPaymentsSpec extends PlaySpec {

  "ReceiveCashPayments" must {

    val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))

    "roundtrip through json" in {
      val data = ReceiveCashPayments(Some(paymentMethods))
      ReceiveCashPayments.jsonR.reads(ReceiveCashPayments.jsonW.writes(data)) mustEqual JsSuccess(
        data,
        JsPath \ "paymentMethods"
      )
    }

    "roundtrip through json1" in {
      val data = ReceiveCashPayments(None)
      ReceiveCashPayments.jsonR.reads(ReceiveCashPayments.jsonW.writes(data)) mustEqual JsSuccess(data)
    }
  }

  "RecieveCashPayments Serialisation" when {
    "paymentsMethods is empty" must {
      "set the receivePayments to false and the payment methods to an empty object" in {
        val res = Json.toJson(ReceiveCashPayments(None))
        res mustBe Json.obj(
          "receivePayments" -> false,
          "paymentMethods"  -> Json.obj()
        )
      }
    }
  }

}
