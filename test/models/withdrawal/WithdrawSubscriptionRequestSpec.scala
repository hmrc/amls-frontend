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

package models.withdrawal

import org.joda.time.{LocalDate, LocalDateTime}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class WithdrawSubscriptionRequestSpec extends PlaySpec with MustMatchers {

  "The withdrawal subscription request object" when {
    "serialised" must {
      "produce the correct JSON" in {
        val date = LocalDate.now()
        val expectedJson = Json.obj(
          "acknowledgementReference" -> "SomeRef",
          "withdrawalDate" -> date.toString("yyyy-MM-dd"),
          "withdrawalReason" -> WithdrawalReason.OutOfScope
        )

        Json.toJson(WithdrawSubscriptionRequest("SomeRef", date, WithdrawalReason.OutOfScope, None)) mustBe expectedJson
      }
    }
  }

}
