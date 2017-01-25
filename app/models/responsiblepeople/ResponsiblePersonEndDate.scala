package models.responsiblepeople

import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import jto.validation._
import jto.validation.forms._
import play.api.libs.json.Json

case class ResponsiblePersonEndDate(endDate: LocalDate)

object ResponsiblePersonEndDate {

  implicit val format = Json.format[ResponsiblePersonEndDate]

  implicit val formRule: Rule[UrlFormEncoded, ResponsiblePersonEndDate] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    __.read(peopleEndDateRule) fmap ResponsiblePersonEndDate.apply
  }

  implicit val formWrites: Write[ResponsiblePersonEndDate, UrlFormEncoded] =
    Write {
      case ResponsiblePersonEndDate(b) => Map(
        "endDate.day" -> Seq(b.get(DateTimeFieldType.dayOfMonth()).toString),
        "endDate.month" -> Seq(b.get(DateTimeFieldType.monthOfYear()).toString),
        "endDate.year" -> Seq(b.get(DateTimeFieldType.year()).toString)
      )
    }
}
