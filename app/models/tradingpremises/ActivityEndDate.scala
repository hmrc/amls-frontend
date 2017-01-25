package models.tradingpremises

import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json

case class ActivityEndDate (endDate: LocalDate)

object ActivityEndDate {

  implicit val format =  Json.format[ActivityEndDate]

  implicit val formRule: Rule[UrlFormEncoded, ActivityEndDate] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    __.read(premisesEndDateRule) fmap ActivityEndDate.apply
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
