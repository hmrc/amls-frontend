package models.tradingpremises

import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class ActivityEndDate (endDate: LocalDate)

object ActivityEndDate {

  implicit val format =  Json.format[ActivityEndDate]

  implicit val formRule: Rule[UrlFormEncoded, ActivityEndDate] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
      (__ \ "endDate").read(localDateFutureRule) fmap ActivityEndDate.apply
  }

  implicit val formWrites: Write[ActivityEndDate, UrlFormEncoded] =
    Write {
      case ActivityEndDate(b) =>Map(
        "endDate.day" -> Seq(b.get(DateTimeFieldType.dayOfMonth()).toString),
        "endDate.month" -> Seq(b.get(DateTimeFieldType.monthOfYear()).toString),
        "endDate.year" -> Seq(b.get(DateTimeFieldType.year()).toString)
      )
    }
}
