package models

import models.FormTypes._
import models.tradingpremises.TradingPremises
import org.joda.time.LocalDate
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, Write}
import play.api.libs.json._

case class DateOfChange (dateOfChange: LocalDate)

object DateOfChange {

  val errorPath = Path \ "dateOfChange"

  implicit val reads: Reads[DateOfChange] =
    __.read[LocalDate] map {
      DateOfChange(_)
    }

  implicit val writes = Writes[DateOfChange] {
    case DateOfChange(b) => Json.toJson(b)
  }

  implicit val formRule: Rule[UrlFormEncoded, DateOfChange] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    __.read(dateOfChangeActivityStartDateRule) fmap DateOfChange.apply
  }

  implicit val formWrites: Write[DateOfChange, UrlFormEncoded] =
    Write {
      case DateOfChange(b) =>Map(
        "dateOfChange.day" -> Seq(""),
        "dateOfChange.month" -> Seq(""),
        "dateOfChange.year" -> Seq("")
      )
    }
}
