package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait PersonHistory

object PersonHistory {

  case object First extends PersonHistory
  case object Second extends PersonHistory
  case object Third extends PersonHistory
  case object Fourth extends PersonHistory

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PersonHistory] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (__ \ "personHistory").read[String].withMessage("error.required.ba.turnover.from.mlr") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case _ =>
        (Path \ "personHistory") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[PersonHistory, UrlFormEncoded] = Write {
    case First => "personHistory" -> "01"
    case Second => "personHistory" -> "02"
    case Third => "personHistory" -> "03"
    case Fourth => "personHistory" -> "04"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "personHistory").read[String].flatMap[PersonHistory] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case _ =>
        ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[PersonHistory] {
    case First => Json.obj("personHistory" -> "01")
    case Second => Json.obj("personHistory" -> "02")
    case Third => Json.obj("personHistory" -> "03")
    case Fourth => Json.obj("personHistory" -> "04")
  }
}