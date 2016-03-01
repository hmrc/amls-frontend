package models.businessactivities

import models.aboutyou.Other
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.i18n.{Messages, Lang}
import play.api.libs.json._

sealed trait TurnerOverExpectIn12Months

case object First extends TurnerOverExpectIn12Months
case object Second extends TurnerOverExpectIn12Months
case object Third extends TurnerOverExpectIn12Months
case object Fourth extends TurnerOverExpectIn12Months
case object Five extends TurnerOverExpectIn12Months
case object Six extends TurnerOverExpectIn12Months
case object Seven extends TurnerOverExpectIn12Months


object TurnerOverExpectIn12Months {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, TurnerOverExpectIn12Months] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (__ \ "turnoverOverExpectIn12MOnths").read[String] flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Five
      case "06" => Six
      case "07" => Seven
      case _ =>
        (Path \ "turnoverOverExpectIn12MOnths") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[TurnerOverExpectIn12Months, UrlFormEncoded] = Write {
    case First => "turnoverOverExpectIn12MOnths" -> "01"
    case Second => "turnoverOverExpectIn12MOnths" -> "02"
    case Third => "turnoverOverExpectIn12MOnths" -> "03"
    case Fourth => "turnoverOverExpectIn12MOnths" -> "04"
    case Five => "turnoverOverExpectIn12MOnths" -> "05"
    case Six => "turnoverOverExpectIn12MOnths" -> "06"
    case Seven => "turnoverOverExpectIn12MOnths" -> "07"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "turnoverOverExpectIn12MOnths").read[String].flatMap[TurnerOverExpectIn12Months] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Five
      case "06" => Six
      case "07" => Seven
      case _ =>
        ValidationError("error.invalid")
    }
  }


  implicit val jsonWrites = Writes[TurnerOverExpectIn12Months] {
    case First => Json.obj("turnoverOverExpectIn12MOnths" -> "01")
    case Second => Json.obj("turnoverOverExpectIn12MOnths" -> "02")
    case Third => Json.obj("turnoverOverExpectIn12MOnths" -> "03")
    case Fourth => Json.obj("turnoverOverExpectIn12MOnths" -> "04")
    case Five => Json.obj("turnoverOverExpectIn12MOnths" -> "05")
    case Six => Json.obj("turnoverOverExpectIn12MOnths" -> "06")
    case Seven => Json.obj("turnoverOverExpectIn12MOnths" -> "07")


  }
}