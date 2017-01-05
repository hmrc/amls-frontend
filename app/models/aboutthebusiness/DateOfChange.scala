package models.aboutthebusiness

import models.FormTypes.localDateFutureRule
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.data.mapping.{From, Rule, Write}
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.Json

case class DateOfChange (dateOfChange: LocalDate)

object DateOfChange {

  implicit val format =  Json.format[DateOfChange]

  implicit val formRule: Rule[UrlFormEncoded, DateOfChange] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "dateOfChange").read(localDateFutureRule) fmap DateOfChange.apply
  }

  implicit val formWrites: Write[DateOfChange, UrlFormEncoded] =
    Write {
      case DateOfChange(b) =>Map(
        "dateOfChange.day" -> Seq(b.get(DateTimeFieldType.dayOfMonth()).toString),
        "dateOfChange.month" -> Seq(b.get(DateTimeFieldType.monthOfYear()).toString),
        "dateOfChange.year" -> Seq(b.get(DateTimeFieldType.year()).toString)
      )
    }
}


