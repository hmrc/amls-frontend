package models.responsiblepeople

import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class ResponsiblePersonEndDate (endDate: LocalDate)

object ResponsiblePersonEndDate {

  implicit val format =  Json.format[ResponsiblePersonEndDate]

  implicit val formRule: Rule[UrlFormEncoded, ResponsiblePersonEndDate] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "endDate").read(localDateFutureRule) fmap ResponsiblePersonEndDate.apply
  }

  implicit val formWrites: Write[ResponsiblePersonEndDate, UrlFormEncoded] =
    Write {
      case ResponsiblePersonEndDate(b) =>Map(
        "endDate.day" -> Seq(b.get(DateTimeFieldType.dayOfMonth()).toString),
        "endDate.month" -> Seq(b.get(DateTimeFieldType.monthOfYear()).toString),
        "endDate.year" -> Seq(b.get(DateTimeFieldType.year()).toString)
      )
    }
}
