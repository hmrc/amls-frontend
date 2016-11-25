package models.notifications

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
                            _id: IDType
                         ) extends SubjectBuilder {

  def dateReceived = {
    val fmt: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM Y")
    receivedAt.toString(fmt)
  }

  def isRead: Boolean = false

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
  implicit val bsonRead: Reads[IDType] =
    (__ \ "$oid").read[String].map { dateTime =>
      new IDType(dateTime)
    }


  implicit val bsonReadWrite: Writes[IDType] = new Writes[IDType] {
    def writes(dateTime: IDType): JsValue = Json.obj(
      "$oid" -> dateTime.id
    )
  }
}