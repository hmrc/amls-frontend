package models.responsiblepeople

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, Write}
import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.libs.json._

case class NewHomeDateOfChange (dateOfChange: LocalDate)

object NewHomeDateOfChange {

  val errorPath = Path \ "dateOfChange"

  val key = "new-home-date-of-change"

  implicit val format = Json.format[NewHomeDateOfChange]

  implicit val formRule: Rule[UrlFormEncoded, NewHomeDateOfChange] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    __.read(dateOfChangeActivityStartDateRule) map NewHomeDateOfChange.apply
  }

  implicit val formWrites: Write[NewHomeDateOfChange, UrlFormEncoded] =
    Write {
      case NewHomeDateOfChange(b) =>Map(
        "dateOfChange.day" -> Seq(b.get(DateTimeFieldType.dayOfMonth()).toString),
        "dateOfChange.month" -> Seq(b.get(DateTimeFieldType.monthOfYear()).toString),
        "dateOfChange.year" -> Seq(b.get(DateTimeFieldType.year()).toString)
      )
    }
}
