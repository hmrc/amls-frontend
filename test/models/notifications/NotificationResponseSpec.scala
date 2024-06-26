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

package models.notifications

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import java.time.LocalDateTime

class NotificationResponseSpec extends AnyWordSpec with Matchers {
  private val testProcessingDate = LocalDateTime.of(2001, 12, 17, 9, 30, 47)

  val notificationJson = Json.obj(
    "processingDate" -> "2001-12-17T09:30:47Z",
    "secureCommText" -> "Some text"
  )

  val notificationModel = NotificationResponse(testProcessingDate, "Some text")

  "Notification Response Serialisation" must {
    "NotificationResponse" must {
      "correctly deserialise" in {
        notificationJson.as[NotificationResponse] must be(notificationModel)
      }
    }
  }
}
