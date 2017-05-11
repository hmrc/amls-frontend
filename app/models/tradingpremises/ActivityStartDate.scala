package models.tradingpremises

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.libs.json.Json

case class ActivityStartDate (startDate: LocalDate)

object ActivityStartDate {

  implicit val format =  Json.format[ActivityStartDate]

  implicit val formRule: Rule[UrlFormEncoded, ActivityStartDate] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
      (__ \ "startDate").read(localDateFutureRule) map ActivityStartDate.apply
  }

  implicit val formWrites: Write[ActivityStartDate, UrlFormEncoded] =
    Write {
      case ActivityStartDate(b) =>Map(
        "startDate.day" -> Seq(b.get(DateTimeFieldType.dayOfMonth()).toString),
        "startDate.month" -> Seq(b.get(DateTimeFieldType.monthOfYear()).toString),
        "startDate.year" -> Seq(b.get(DateTimeFieldType.year()).toString)
      )
    }

}
