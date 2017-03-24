package models.notifications

import models.notifications.ContactType.{ApplicationAutorejectionForFailureToPay, RegistrationVariationApproval}
import models.notifications.RejectedReason.FailedToPayCharges
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Writes, _}

case class NotificationRow(
                            status: Option[Status],
                            contactType: Option[ContactType],
                            contactNumber: Option[String],
                            variation: Boolean,
                            receivedAt: DateTime,
                            isRead: Boolean,
                            _id: IDType
                          ) extends SubjectBuilder {

  def dateReceived = {
    val fmt: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM Y")
    receivedAt.toString(fmt)
  }

  def getContactType: ContactType = {

    val statusReason = for {
      st <- status
      reason <- st.statusReason
    } yield reason


    contactType.getOrElse(
      (statusReason, variation) match {
        case (Some(r), _) => ApplicationAutorejectionForFailureToPay
        case (_, true) => RegistrationVariationApproval
        case _ => throw new RuntimeException("No matching ContactType found for id " + _id)
      }

    )
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
