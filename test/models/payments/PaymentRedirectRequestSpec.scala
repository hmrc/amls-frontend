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

import config.ApplicationConfig
import models.ReturnLocation
import play.api.libs.json.Json
import utils.AmlsSpec

class PaymentRedirectRequestSpec extends AmlsSpec {

  "The PaymentRedirectRequest type" must {

    "serialize to the correct JSON format" in {

      val expectedJson = Json.obj(
        "reference" -> "some_reference",
        "amount"    -> "100.0",
        "url"       -> "http://localhost:9222/anti-money-laundering/start"
      )

      // noinspection ScalaStyle

      val applicationConfig = app.injector.instanceOf[ApplicationConfig]

      val model =
        PaymentRedirectRequest("some_reference", 100, ReturnLocation("/anti-money-laundering/start")(applicationConfig))

      Json.toJson(model) mustBe expectedJson

    }

  }

}
