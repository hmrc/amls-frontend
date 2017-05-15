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

package models.notifications

import models.notifications.StatusType.{Rejected,Revoked,DeRegistered}
import play.api.libs.json._

case class Status(status: Option[StatusType], statusReason: Option[StatusReason])

object Status {

  implicit val jsonReads: Reads[Status] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

    ((__ \ "status_type").readNullable[StatusType] and
      (__ \ "status_reason").readNullable[String]).tupled map {
      case (Some(status), Some(reason)) => status match {
        case Rejected => Status(Some(status), Some(RejectedReason.reason(reason)))
        case Revoked => Status(Some(status), Some(RevokedReason.reason(reason)))
        case DeRegistered => Status(Some(status), Some(DeregisteredReason.reason(reason)))
        case _ => Status(Some(status), None)
      }
      case (Some(status), None) => Status(Some(status), None)
      case _ => Status(None, None)
    }
  }


  implicit val jsonWrites: Writes[Status] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "status_type").writeNullable[StatusType] and
        (__ \ "status_reason").writeNullable[StatusReason]
      ) (unlift(Status.unapply))
  }
}
