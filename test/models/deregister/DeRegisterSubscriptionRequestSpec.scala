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

package models.deregister

import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class DeRegisterSubscriptionRequestSpec extends PlaySpec with MustMatchers {

  "The model" must {
    "deserialise correctly" in {
      val reference = "A" * 32

      val expectedJson = Json.obj(
        "acknowledgementReference" -> reference,
        "deregistrationDate" -> LocalDate.now.toString("yyyy-MM-dd"),
        "deregistrationReason" -> DeRegisterReason.OutOfScope
      )

      Json.toJson(DeRegisterSubscriptionRequest(reference, LocalDate.now, DeRegisterReason.OutOfScope)) mustBe expectedJson
    }
  }

}
