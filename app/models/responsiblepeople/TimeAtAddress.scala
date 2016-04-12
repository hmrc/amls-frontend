package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait TimeAtAddress

object TimeAtAddress {

  case object ZeroToFiveMonths extends TimeAtAddress
  case object SixToElevenMonths extends TimeAtAddress
  case object OneToThreeYears extends TimeAtAddress
  case object ThreeYearsPlus extends TimeAtAddress

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, TimeAtAddress] = From[UrlFormEncoded] { __ =>

    import play.api.data.mapping.forms.Rules._

    (__ \ "timeAtAddress").read[String].withMessage("error.required.rp.wherepersonlives.howlonglived") flatMap {
      case "01" => ZeroToFiveMonths
      case "02" => SixToElevenMonths
      case "03" => OneToThreeYears
      case "04" => ThreeYearsPlus
      case _ =>
        (Path \ "timeAtAddress") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[TimeAtAddress, UrlFormEncoded] = Write {
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
          ValidationError("error.invalid")
      }
    }

  implicit val jsonWrites = Writes[TimeAtAddress] {
      case ZeroToFiveMonths => Json.obj("timeAtAddress" -> "01")
      case SixToElevenMonths => Json.obj("timeAtAddress" -> "02")
      case OneToThreeYears => Json.obj("timeAtAddress" -> "03")
      case ThreeYearsPlus => Json.obj("timeAtAddress" -> "04")
    }
}