package models.businessactivities

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.i18n.{Messages, Lang}
import play.api.libs.json._

sealed trait ExpectedBusinessTurnover


object ExpectedBusinessTurnover {

  case object First extends ExpectedBusinessTurnover
  case object Second extends ExpectedBusinessTurnover
  case object Third extends ExpectedBusinessTurnover
  case object Fourth extends ExpectedBusinessTurnover
  case object Fifth extends ExpectedBusinessTurnover
  case object Sixth extends ExpectedBusinessTurnover
  case object Seventh extends ExpectedBusinessTurnover

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ExpectedBusinessTurnover] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (__ \ "expectedBusinessTurnover").read[String].withMessage("error.required.ba.business.turnover") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        (Path \ "expectedBusinessTurnover") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[ExpectedBusinessTurnover, UrlFormEncoded] = Write {
    case First => "expectedBusinessTurnover" -> "01"
    case Second => "expectedBusinessTurnover" -> "02"
    case Third => "expectedBusinessTurnover" -> "03"
    case Fourth => "expectedBusinessTurnover" -> "04"
    case Fifth => "expectedBusinessTurnover" -> "05"
    case Sixth => "expectedBusinessTurnover" -> "06"
    case Seventh => "expectedBusinessTurnover" -> "07"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "expectedBusinessTurnover").read[String].flatMap[ExpectedBusinessTurnover] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        ValidationError("error.invalid")
    }
  }


  implicit val jsonWrites = Writes[ExpectedBusinessTurnover] {
    case First => Json.obj("expectedBusinessTurnover" -> "01")
    case Second => Json.obj("expectedBusinessTurnover" -> "02")
    case Third => Json.obj("expectedBusinessTurnover" -> "03")
    case Fourth => Json.obj("expectedBusinessTurnover" -> "04")
    case Fifth => Json.obj("expectedBusinessTurnover" -> "05")
    case Sixth => Json.obj("expectedBusinessTurnover" -> "06")
    case Seventh => Json.obj("expectedBusinessTurnover" -> "07")


  }
}