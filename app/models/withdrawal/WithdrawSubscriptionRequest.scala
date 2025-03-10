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

package models.withdrawal

import java.time.LocalDate

case class WithdrawSubscriptionRequest(
  acknowledgementReference: String,
  withdrawalDate: LocalDate,
  withdrawalReason: WithdrawalReason,
  withdrawalReasonOthers: Option[String] = None
)

object WithdrawSubscriptionRequest {

  import play.api.libs.json._

  val DefaultAckReference: String = "A" * 32

  implicit val format: Reads[WithdrawSubscriptionRequest] = Json.reads[WithdrawSubscriptionRequest]

  implicit val jsonWrites: Writes[WithdrawSubscriptionRequest] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    Writes[WithdrawSubscriptionRequest] { ep =>
      (
        (__ \ "acknowledgementReference").write[String] and
          (__ \ "withdrawalDate").write[LocalDate] and
          __.write[WithdrawalReason] and
          (__ \ "withdrawalReasonOthers").writeNullable[String]
      )(unlift(WithdrawSubscriptionRequest.unapply)).writes(ep)
    }
  }
}
