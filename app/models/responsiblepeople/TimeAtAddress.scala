package models.responsiblepeople

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json._

sealed trait TimeAtAddress

object TimeAtAddress {

  case object Empty extends TimeAtAddress
  case object ZeroToFiveMonths extends TimeAtAddress
  case object SixToElevenMonths extends TimeAtAddress
  case object OneToThreeYears extends TimeAtAddress
  case object ThreeYearsPlus extends TimeAtAddress

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, TimeAtAddress] = From[UrlFormEncoded] { __ =>

    import jto.validation.forms.Rules._

    (__ \ "timeAtAddress").read[String].withMessage("error.required.timeAtAddress") flatMap {
      case "" => (Path \ "timeAtAddress") -> Seq(ValidationError("error.required.timeAtAddress"))
      case "01" => ZeroToFiveMonths
      case "02" => SixToElevenMonths
      case "03" => OneToThreeYears
      case "04" => ThreeYearsPlus
      case _ =>
        (Path \ "timeAtAddress") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[TimeAtAddress, UrlFormEncoded] = Write {
    case Empty => Map.empty
    case ZeroToFiveMonths => "timeAtAddress" -> "01"
    case SixToElevenMonths => "timeAtAddress" -> "02"
    case OneToThreeYears => "timeAtAddress" -> "03"
    case ThreeYearsPlus => "timeAtAddress" -> "04"
  }

  implicit val jsonReads: Reads[TimeAtAddress] = {
      import play.api.libs.json.Reads.StringReads
      (__ \ "timeAtAddress").read[String].flatMap[TimeAtAddress] {
        case "01" => ZeroToFiveMonths
        case "02" => SixToElevenMonths
        case "03" => OneToThreeYears
        case "04" => ThreeYearsPlus
        case _ =>
          play.api.data.validation.ValidationError("error.invalid")
      }
    }

  implicit val jsonWrites = Writes[TimeAtAddress] {
      case Empty => JsNull
      case ZeroToFiveMonths => Json.obj("timeAtAddress" -> "01")
      case SixToElevenMonths => Json.obj("timeAtAddress" -> "02")
      case OneToThreeYears => Json.obj("timeAtAddress" -> "03")
      case ThreeYearsPlus => Json.obj("timeAtAddress" -> "04")
    }
}
