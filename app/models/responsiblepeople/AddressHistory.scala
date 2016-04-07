package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait AddressHistory

object AddressHistory {

  case object First extends AddressHistory
  case object Second extends AddressHistory
  case object Third extends AddressHistory
  case object Fourth extends AddressHistory

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, AddressHistory] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (__ \ "addressHistory").read[String].withMessage("error.required.rp.wherepersonlives.howlonglived") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case _ =>
        (Path \ "addressHistory") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[AddressHistory, UrlFormEncoded] = Write {
    case First => "addressHistory" -> "01"
    case Second => "addressHistory" -> "02"
    case Third => "addressHistory" -> "03"
    case Fourth => "addressHistory" -> "04"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "addressHistory").read[String].flatMap[AddressHistory] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case _ =>
        ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[AddressHistory] {
    case First => Json.obj("addressHistory" -> "01")
    case Second => Json.obj("addressHistory" -> "02")
    case Third => Json.obj("addressHistory" -> "03")
    case Fourth => Json.obj("addressHistory" -> "04")
  }
}