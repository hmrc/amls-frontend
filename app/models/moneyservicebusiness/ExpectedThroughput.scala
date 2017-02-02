package models.moneyservicebusiness

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json._

sealed trait ExpectedThroughput

object ExpectedThroughput {

  case object First extends ExpectedThroughput
  case object Second extends ExpectedThroughput
  case object Third extends ExpectedThroughput
  case object Fourth extends ExpectedThroughput
  case object Fifth extends ExpectedThroughput
  case object Sixth extends ExpectedThroughput
  case object Seventh extends ExpectedThroughput


  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ExpectedThroughput] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "throughput").read[String].withMessage("error.required.msb.throughput") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        (Path \ "throughput") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[ExpectedThroughput, UrlFormEncoded] = Write {
    case First => "throughput" -> "01"
    case Second => "throughput" -> "02"
    case Third => "throughput" -> "03"
    case Fourth => "throughput" -> "04"
    case Fifth => "throughput" -> "05"
    case Sixth => "throughput" -> "06"
    case Seventh=> "throughput" -> "07"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "throughput").read[String].flatMap[ExpectedThroughput] {
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

  implicit val jsonWrites = Writes[ExpectedThroughput] {
    case First => Json.obj("throughput" -> "01")
    case Second => Json.obj("throughput" -> "02")
    case Third => Json.obj("throughput" -> "03")
    case Fourth => Json.obj("throughput" -> "04")
    case Fifth => Json.obj("throughput" -> "05")
    case Sixth => Json.obj("throughput" -> "06")
    case Seventh => Json.obj("throughput" -> "07")
  }
}
