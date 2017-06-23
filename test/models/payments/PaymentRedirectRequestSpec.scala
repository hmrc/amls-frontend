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

package models.payments

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class PaymentRedirectRequestSpec extends PlaySpec {

  "The PaymentRedirectRequest type" must {

    "serialize to the correct JSON format" in {

      implicit val request = FakeRequest(GET, "http://localhost:9222/anti-money-laundering")

      val expectedJson = Json.obj(
        "reference" -> "some_reference",
        "amount" -> "100.0",
        "url" -> "http://localhost:9222/anti-money-laundering/start"
      )

      val model = PaymentRedirectRequest("some_reference", 100, ReturnLocation("/anti-money-laundering/start"))

      Json.toJson(model) mustBe expectedJson

    }

  }

}
