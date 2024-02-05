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

import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import utils.AmlsSpec

class PaymentSpec extends AmlsSpec with ScalaFutures {

  "Payment" must {
    "round trip through JSON formatting" in {
      val amlsBackendPaymentJson =
        """
          |{
          |  "_id": "123456789",
          |  "amlsRefNo": "X12345678",
          |  "safeId": "X73289473",
          |  "reference": "X987654321",
          |  "amountInPence": 10000,
          |  "status": "Successful",
          |  "isBacs": true,
          |  "createdAt": {
          |    "$date": {
          |      "$numberLong": "1577836805555"
          |    }
          |  },
          |  "updatedAt": "2020-01-02T00:00:05.555"
          |}
          |""".stripMargin

      Json.toJson(amlsBackendPaymentJson)
    }
  }
}