package models

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTimeZone, LocalDate, LocalDateTime}
import play.api.libs.json._

case class ReadStatusResponse(
                               processingDate: LocalDateTime,
                               formBundleStatus: String,
                               statusReason: Option[String],
                               deRegistrationDate: Option[LocalDate],
                               currentRegYearStartDate: Option[LocalDate],
                               currentRegYearEndDate: Option[LocalDate],
                               renewalConFlag: Boolean,
                               renewalSubmissionFlag: Option[Boolean] = None,
                               currentAMLSOutstandingBalance: Option[String] = None,
                               businessContactNumber: Option[String] = None
                             )

object ReadStatusResponse {

  val dateTimeFormat = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC

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

