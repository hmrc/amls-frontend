package models

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait SatisfactionSurvey

object SatisfactionSurvey {

  case object First extends SatisfactionSurvey
  case object Second extends SatisfactionSurvey
  case object Third extends SatisfactionSurvey
  case object Fourth extends SatisfactionSurvey
  case object Fifth extends SatisfactionSurvey

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, SatisfactionSurvey] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (__ \ "satisfactionSurvey").read[String].withMessage("") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case _ =>
        (Path \ "satisfactionSurvey") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[SatisfactionSurvey, UrlFormEncoded] = Write {
    case First => "satisfactionSurvey" -> "01"
    case Second => "satisfactionSurvey" -> "02"
    case Third => "satisfactionSurvey" -> "03"
    case Fourth => "satisfactionSurvey" -> "04"
    case Fifth => "satisfactionSurvey" -> "05"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "satisfactionSurvey").read[String].flatMap[SatisfactionSurvey] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case _ =>
        ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[SatisfactionSurvey] {
    case First => Json.obj("satisfactionSurvey" -> "01")
    case Second => Json.obj("satisfactionSurvey" -> "02")
    case Third => Json.obj("satisfactionSurvey" -> "03")
    case Fourth => Json.obj("satisfactionSurvey" -> "04")
    case Fifth => Json.obj("satisfactionSurvey" -> "05")
  }
}
