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

package models.deregister

import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DeRegisterSubscriptionRequestSpec extends PlaySpec with Matchers {

  "The model" must {
    "deserialise correctly" in {
      val reference = "A" * 32

      val expectedJson = Json.obj(
        "acknowledgementReference" -> reference,
        "deregistrationDate"       -> LocalDate.now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
        "deregistrationReason"     -> "Out of scope"
      )

      Json.toJson(
        DeRegisterSubscriptionRequest(reference, LocalDate.now, DeregistrationReason.OutOfScope)
      ) mustBe expectedJson
    }
  }

}
