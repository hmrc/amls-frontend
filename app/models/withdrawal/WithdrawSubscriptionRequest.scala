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

import org.joda.time.LocalDate

object WithdrawalReason {
  val OutOfScope = "Out of scope"
}

case class WithdrawSubscriptionRequest (acknowledgementReference: String,
                                        withdrawalDate: LocalDate,
                                        withdrawalReason: String,
                                        withdrawalReasonOthers: Option[String] = None
                                       )

object WithdrawSubscriptionRequest {
  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val DefaultAckReference = "A" * 32

  implicit val reads: Reads[WithdrawSubscriptionRequest] = {
    (
      (__ \ "acknowledgementReference").read[String] and
        (__ \ "withdrawalDate").read[String].map[LocalDate](LocalDate.parse) and
        (__ \ "withdrawalReason").read[String] and
        (__ \ "withdrawalReasonOthers").readNullable[String]
    )(WithdrawSubscriptionRequest.apply _)
  }

  implicit val writes: Writes[WithdrawSubscriptionRequest] = {
    (
      (__ \ "acknowledgementReference").write[String] and
        (__ \ "withdrawalDate").write[String].contramap[LocalDate](_.toString("yyyy-MM-dd")) and
        (__ \ "withdrawalReason").write[String] and
        (__ \ "withdrawalReasonOthers").writeNullable[String]
    )(unlift(WithdrawSubscriptionRequest.unapply))
  }

}
