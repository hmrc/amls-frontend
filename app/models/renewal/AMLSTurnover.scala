package models.renewal

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json._

sealed trait AMLSTurnover

object AMLSTurnover {

  val key = "renewal-amls-turnover"

  case object First extends AMLSTurnover
  case object Second extends AMLSTurnover
  case object Third extends AMLSTurnover
  case object Fourth extends AMLSTurnover
  case object Fifth extends AMLSTurnover
  case object Sixth extends AMLSTurnover
  case object Seventh extends AMLSTurnover

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, AMLSTurnover] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import models.FormTypes._
    (__ \ "AMLSTurnover").read[String].withMessage("error.required.ba.turnover.from.mlr") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        (Path \ "AMLSTurnover") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[AMLSTurnover, UrlFormEncoded] = Write {
    case First => "AMLSTurnover" -> "01"
    case Second => "AMLSTurnover" -> "02"
    case Third => "AMLSTurnover" -> "03"
    case Fourth => "AMLSTurnover" -> "04"
    case Fifth => "AMLSTurnover" -> "05"
    case Sixth => "AMLSTurnover" -> "06"
    case Seventh=> "AMLSTurnover" -> "07"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "AMLSTurnover").read[String].flatMap[AMLSTurnover] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        play.api.data.validation.ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[AMLSTurnover] {
    case First => Json.obj("AMLSTurnover" -> "01")
    case Second => Json.obj("AMLSTurnover" -> "02")
    case Third => Json.obj("AMLSTurnover" -> "03")
    case Fourth => Json.obj("AMLSTurnover" -> "04")
    case Fifth => Json.obj("AMLSTurnover" -> "05")
    case Sixth => Json.obj("AMLSTurnover" -> "06")
    case Seventh => Json.obj("AMLSTurnover" -> "07")
  }
}
