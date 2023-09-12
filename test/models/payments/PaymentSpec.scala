/*
 * Copyright 2023 HM Revenue & Customs
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

class PaymentSpec extends AmlsSpec with ScalaFutures { //TODO come back and sort this

  val paymentJson =
    """
      |{
      |  "_id": "59b1204b2e000028005c0442",
      |  "amlsRefNo": "XSML00000200738",
      |  "safeId": "",
      |  "reference": "XH002610109496",
      |  "description": "BACS Payment",
      |  "amountInPence": 31500,
      |  "status": "Created",
      |  "createdAt": "2017-09-07T10:32:43.526",
      |  "isBacs": true
      |}
      |""".stripMargin

  "payment" must {
    "deserialise" in {
      val payment = Json.parse(paymentJson).as[Payment]
      println(s"\nthe payment is: $payment\n")

      //      val payment = Payment("59602100ecfc04ab8ebfddf1c00de1d0", "XQML00000167165", "XIML00000438461", "XHrRexvcfa", Some("some description"),
      //        1000, PaymentStatuses.Created, LocalDateTime.now(), None, None)

      println(s"\ngoing the other way around:\n${Json.prettyPrint(Json.toJson(payment))} \n")

      1 mustEqual 1
    }
  }
}