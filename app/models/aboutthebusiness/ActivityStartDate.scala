package models.aboutthebusiness

import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.data.mapping.{To, Write, From, Rule}
import play.api.data.mapping.forms._
import play.api.libs.json.Json
import models.FormTypes._

case class ActivityStartDate (startDate: LocalDate)

object ActivityStartDate {

  implicit val format =  Json.format[ActivityStartDate]

  implicit val formRule: Rule[UrlFormEncoded, ActivityStartDate] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
      (__ \ "startDate").read(localDateRule) fmap ActivityStartDate.apply
  }

  implicit val formWrites: Write[ActivityStartDate, UrlFormEncoded] =
    Write {
      case ActivityStartDate(b) =>Map(
        "startDate.day" -> Seq(b.get(DateTimeFieldType.dayOfMonth()).toString),
      "startDate.month" -> Seq(b.get(DateTimeFieldType.monthOfYear()).toString),
      "startDate.year" -> Seq(b.get(DateTimeFieldType.year()).toString))
    }

}
