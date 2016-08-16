package models

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTimeZone, LocalDate, LocalDateTime}
import play.api.libs.json._

case class ReadStatusResponse(
                               processingDate: LocalDateTime,
                               formBundleStatus: String,
                               deRegistrationDate: Option[LocalDate],
                               currentRegYearStartDate: Option[LocalDate],
                               currentRegYearEndDate: Option[LocalDate],
                               renewalConFlag: Boolean
                             )

object ReadStatusResponse {

  val dateTimeFormat = ISODateTimeFormat.dateTime.withZoneUTC

  implicit val readsJodaLocalDateTime = Reads[LocalDateTime](js =>
    js.validate[String].map[LocalDateTime](dtString =>
      LocalDateTime.parse(dtString, dateTimeFormat)
    )
  )

  implicit val localDateTimeWrite: Writes[LocalDateTime] = new Writes[LocalDateTime] {
    def writes(dateTime: LocalDateTime): JsValue = JsString(dateTimeFormat.print(dateTime.toDateTime(DateTimeZone.UTC)))
  }

  implicit val format = Json.format[ReadStatusResponse]
}
