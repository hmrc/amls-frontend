package models.notifications

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTimeZone, LocalDateTime}
import play.api.libs.json._

case class NotificationResponse(processingDate: LocalDateTime, secureCommText: String)

object NotificationResponse {

  val dateTimeFormat = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC

  implicit val readsJodaLocalDateTime = Reads[LocalDateTime](js =>
    js.validate[String].map[LocalDateTime](dtString =>
      LocalDateTime.parse(dtString, dateTimeFormat)
    )
  )

  implicit val localDateTimeWrite: Writes[LocalDateTime] = new Writes[LocalDateTime] {
    def writes(dateTime: LocalDateTime): JsValue = JsString(dateTimeFormat.print(dateTime.toDateTime(DateTimeZone.UTC)))
  }

  implicit val format = Json.format[NotificationResponse]
}
