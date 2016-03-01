package models.businessactivities

import models.aboutyou.Other
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.i18n.{Messages, Lang}
import play.api.libs.json._

sealed trait TurnerOverExpectIn12MonthsRelatedToAMLS

case object FirstTurnoverAmls extends TurnerOverExpectIn12MonthsRelatedToAMLS
case object SecondTurnoverAmls extends TurnerOverExpectIn12MonthsRelatedToAMLS
case object ThirdTurnoverAmls extends TurnerOverExpectIn12MonthsRelatedToAMLS
case object FourthTurnoverAmls extends TurnerOverExpectIn12MonthsRelatedToAMLS
case object FivthTurnoverAmls extends TurnerOverExpectIn12MonthsRelatedToAMLS
case object SixthTurnoverAmls extends TurnerOverExpectIn12MonthsRelatedToAMLS
case object SevenTurnoverAmls extends TurnerOverExpectIn12MonthsRelatedToAMLS


object TurnerOverExpectIn12MonthsRelatedToAMLS {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, TurnerOverExpectIn12MonthsRelatedToAMLS] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (__ \ "turnoverOverExpectIn12MOnthsRelatedToAMLS").read[String] flatMap {
      case "01" => FirstTurnoverAmls
      case "02" => SecondTurnoverAmls
      case "03" => ThirdTurnoverAmls
      case "04" => FourthTurnoverAmls
      case "05" => FivthTurnoverAmls
      case "06" => SixthTurnoverAmls
      case "07" => SevenTurnoverAmls
      case _ =>
        (Path \ "turnoverOverExpectIn12MOnthsRelatedToAMLS") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[TurnerOverExpectIn12MonthsRelatedToAMLS, UrlFormEncoded] = Write {
    case FirstTurnoverAmls => "turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "01"
    case SecondTurnoverAmls => "turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "02"
    case ThirdTurnoverAmls => "turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "03"
    case FourthTurnoverAmls => "turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "04"
    case FivthTurnoverAmls => "turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "05"
    case SixthTurnoverAmls => "turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "06"
    case SevenTurnoverAmls => "turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "07"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "turnoverOverExpectIn12MOnthsRelatedToAMLS").read[String].flatMap[TurnerOverExpectIn12MonthsRelatedToAMLS] {
      case "01" => FirstTurnoverAmls
      case "02" => SecondTurnoverAmls
      case "03" => ThirdTurnoverAmls
      case "04" => FourthTurnoverAmls
      case "05" => FivthTurnoverAmls
      case "06" => SixthTurnoverAmls
      case "07" => SevenTurnoverAmls
      case _ =>
        ValidationError("error.invalid")
    }
  }


  implicit val jsonWrites = Writes[TurnerOverExpectIn12MonthsRelatedToAMLS] {
    case FirstTurnoverAmls => Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "01")
    case SecondTurnoverAmls => Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "02")
    case ThirdTurnoverAmls => Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "03")
    case FourthTurnoverAmls => Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "04")
    case FivthTurnoverAmls => Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "05")
    case SixthTurnoverAmls => Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "06")
    case SevenTurnoverAmls => Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "07")


  }
}