package models.renewal

import jto.validation.{ValidationError, _}
import jto.validation.forms.UrlFormEncoded
import models.businessactivities.ExpectedBusinessTurnover
import play.api.libs.json._

sealed trait BusinessTurnover

object BusinessTurnover {

  case object First extends BusinessTurnover
  case object Second extends BusinessTurnover
  case object Third extends BusinessTurnover
  case object Fourth extends BusinessTurnover
  case object Fifth extends BusinessTurnover
  case object Sixth extends BusinessTurnover
  case object Seventh extends BusinessTurnover

  import utils.MappingUtils.Implicits._

  val key = "renewal-business-turnover"

  implicit val formRule: Rule[UrlFormEncoded, BusinessTurnover] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "businessTurnover").read[String].withMessage("error.required.ba.business.turnover") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        (Path \ "businessTurnover") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[BusinessTurnover, UrlFormEncoded] = Write {
    case First => "businessTurnover" -> "01"
    case Second => "businessTurnover" -> "02"
    case Third => "businessTurnover" -> "03"
    case Fourth => "businessTurnover" -> "04"
    case Fifth => "businessTurnover" -> "05"
    case Sixth => "businessTurnover" -> "06"
    case Seventh => "businessTurnover" -> "07"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "businessTurnover").read[String].flatMap[BusinessTurnover] {
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

  implicit val jsonWrites = Writes[BusinessTurnover] {
    case First => Json.obj("businessTurnover" -> "01")
    case Second => Json.obj("businessTurnover" -> "02")
    case Third => Json.obj("businessTurnover" -> "03")
    case Fourth => Json.obj("businessTurnover" -> "04")
    case Fifth => Json.obj("businessTurnover" -> "05")
    case Sixth => Json.obj("businessTurnover" -> "06")
    case Seventh => Json.obj("businessTurnover" -> "07")
  }

  implicit def convert(model: BusinessTurnover): ExpectedBusinessTurnover = model match {
    case BusinessTurnover.First => ExpectedBusinessTurnover.First
    case BusinessTurnover.Second => ExpectedBusinessTurnover.Second
    case BusinessTurnover.Third => ExpectedBusinessTurnover.Third
    case BusinessTurnover.Fourth => ExpectedBusinessTurnover.Fourth
    case BusinessTurnover.Fifth => ExpectedBusinessTurnover.Fifth
    case BusinessTurnover.Sixth => ExpectedBusinessTurnover.Sixth
    case BusinessTurnover.Seventh => ExpectedBusinessTurnover.Seventh
    case _ => throw new Exception("Invalid business turnover value")
  }
}
