/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.json.{Json, Writes}
import play.api.libs.json.JodaReads._

case class DeRegisterSubscriptionRequest(acknowledgementReference: String,
                                         deregistrationDate: LocalDate,
                                         deregistrationReason: DeregistrationReason,
                                         deregReasonOther: Option[String] = None)

object DeRegisterSubscriptionRequest {
  val DefaultAckReference = "A" * 32

  implicit val reads = Json.reads[DeRegisterSubscriptionRequest]

  implicit val writes: Writes[DeRegisterSubscriptionRequest] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    import play.api.libs.json.JodaWrites._
    Writes[DeRegisterSubscriptionRequest] { ep =>
      (
        (__ \ "acknowledgementReference").write[String] and
          (__ \ "deregistrationDate").write[LocalDate] and
          __.write[DeregistrationReason] and
          (__ \ "deregReasonOther").writeNullable[String]
        ) (unlift(DeRegisterSubscriptionRequest.unapply)).writes(ep)
    }
  }
}
