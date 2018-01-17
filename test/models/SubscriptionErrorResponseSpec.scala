/*
 * Copyright 2018 HM Revenue & Customs
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

package models

import generators.AmlsReferenceNumberGenerator
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class SubscriptionErrorResponseSpec extends PlaySpec with MustMatchers with AmlsReferenceNumberGenerator {

  "Deserializing" must {
    "produce the correct model" in {
      val json = Json.obj(
        "amlsRegNumber" -> amlsRegistrationNumber,
        "message" -> "There was an error during subscription"
      )

      json.as[SubscriptionErrorResponse] mustBe SubscriptionErrorResponse(amlsRegistrationNumber, "There was an error during subscription")
    }
  }

  "Serializing" must {
    "produce the correct Json" in {
      val model = SubscriptionErrorResponse(amlsRegistrationNumber, "An error")

      Json.toJson(model) mustBe Json.obj(
        "amlsRegNumber" -> amlsRegistrationNumber,
        "message" -> "An error"
      )
    }
  }

}
