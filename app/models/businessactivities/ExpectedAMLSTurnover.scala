package models.businessactivities

import models.aboutyou.Other
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.i18n.{Messages, Lang}
import play.api.libs.json._

sealed trait ExpectedAMLSTurnover

object ExpectedAMLSTurnover {

  case object First extends ExpectedAMLSTurnover
  case object Second extends ExpectedAMLSTurnover
  case object Third extends ExpectedAMLSTurnover
  case object Fourth extends ExpectedAMLSTurnover
  case object Fifth extends ExpectedAMLSTurnover
  case object Sixth extends ExpectedAMLSTurnover
  case object Seventh extends ExpectedAMLSTurnover



  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ExpectedAMLSTurnover] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (__ \ "expectedAMLSTurnover").read[String] flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        (Path \ "expectedAMLSTurnover") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[ExpectedAMLSTurnover, UrlFormEncoded] = Write {
    case First => "expectedAMLSTurnover" -> "01"
    case Second => "expectedAMLSTurnover" -> "02"
    case Third => "expectedAMLSTurnover" -> "03"
    case Fourth => "expectedAMLSTurnover" -> "04"
    case Fifth => "expectedAMLSTurnover" -> "05"
    case Sixth => "expectedAMLSTurnover" -> "06"
    case Seventh=> "expectedAMLSTurnover" -> "07"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "expectedAMLSTurnover").read[String].flatMap[ExpectedAMLSTurnover] {
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


  implicit val jsonWrites = Writes[ExpectedAMLSTurnover] {
    case First => Json.obj("expectedAMLSTurnover" -> "01")
    case Second => Json.obj("expectedAMLSTurnover" -> "02")
    case Third => Json.obj("expectedAMLSTurnover" -> "03")
    case Fourth => Json.obj("expectedAMLSTurnover" -> "04")
    case Fifth => Json.obj("expectedAMLSTurnover" -> "05")
    case Sixth => Json.obj("expectedAMLSTurnover" -> "06")
    case Seventh => Json.obj("expectedAMLSTurnover" -> "07")


  }
}