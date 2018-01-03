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

package models.notifications

import models.notifications.ContactType._
import models.notifications.StatusType.DeRegistered
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{JsValue, Writes, _}
import RejectedReason._

case class NotificationRow(
                            status: Option[Status],
                            contactType: Option[ContactType],
                            contactNumber: Option[String],
                            variation: Boolean,
                            receivedAt: DateTime,
                            isRead: Boolean,
                            amlsRegistrationNumber: String,
                            _id: IDType
                          ) {

  def dateReceived: String = {
    val fmt: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM Y")
    receivedAt.toString(fmt)
  }

  def getContactType: ContactType = {

    val statusReason = for {
      st <- status
      reason <- st.statusReason
    } yield reason

    contactType.getOrElse(
      (status, statusReason, variation) match {
        case (Some(Status(Some(DeRegistered), _)), _, _) => DeRegistrationEffectiveDateChange
        case (_, Some(_), _) => ApplicationAutorejectionForFailureToPay
        case (_, _, true) => RegistrationVariationApproval
        case _ => NoSubject
      }
    )

  }

  def subject: String = contactType match {
    case Some(RejectionReasons) => {
      (for {
        st <- status
        sr <- st.statusReason match {
          case Some(NonCompliant) | Some(FitAndProperFailure) | Some(OtherRefused) => Some("notifications.rejr.title")
          case _ => None
        }
      } yield sr) getOrElse "notifications.fail.title"
    }
    case _ => {
      getContactType match {
        case ApplicationAutorejectionForFailureToPay => "notifications.fail.title"
        case _ => s"notifications.subject.$getContactType"
      }
    }
  }

}

object NotificationRow {

  implicit val dateTimeRead: Reads[DateTime] = {
    (__ \ "$date").read[Long].map { dateTime =>
      new DateTime(dateTime, DateTimeZone.UTC)
    }
  }

  implicit val dateTimeWrite: Writes[DateTime] = new Writes[DateTime] {
    def writes(dateTime: DateTime): JsValue = Json.obj(
      "$date" -> dateTime.getMillis
    )
  }

  implicit val format = Json.format[NotificationRow]
}

case class IDType(id: String)

object IDType {

  implicit val read: Reads[IDType] =
    (__ \ "$oid").read[String].map { dateTime =>
      new IDType(dateTime)
    }

  implicit val write: Writes[IDType] = new Writes[IDType] {
    def writes(dateTime: IDType): JsValue = Json.obj(
      "$oid" -> dateTime.id
    )
  }

}
