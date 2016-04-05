package models.tradingpremises

import play.api.data.mapping.{Write, Path, From, Rule}
import play.api.data.mapping.forms._
import play.api.data.validation.ValidationError
import play.api.i18n.{Messages, Lang}
import play.api.libs.json.Writes
import play.api.libs.json._


sealed trait BusinessStructure {
  def message(implicit lang: Lang): String =
    this match {
      case SoleProprietor =>
        Messages("tradingpremises.youragent.businessstructure.lbl.01")
      case LimitedLiabilityPartnership =>
        Messages("tradingpremises.youragent.businessstructure.lbl.02")
      case Partnership =>
        Messages("tradingpremises.youragent.businessstructure.lbl.03")
      case IncorporatedBody =>
        Messages("tradingpremises.youragent.businessstructure.lbl.04")
      case UnincorporatedBody =>
        Messages("tradingpremises.youragent.businessstructure.lbl.05")
    }
}

case object SoleProprietor extends BusinessStructure
case object LimitedLiabilityPartnership extends BusinessStructure
case object Partnership extends BusinessStructure
case object IncorporatedBody extends BusinessStructure
case object UnincorporatedBody extends BusinessStructure


object BusinessStructure {

  import utils.MappingUtils.Implicits._

  implicit val jsonReadsBusinessStructure = {
    (__ \ "agentsBusinessStructure").read[String].flatMap[BusinessStructure] {
      case "01" => SoleProprietor
      case "02" => LimitedLiabilityPartnership
      case "03" => Partnership
      case "04" => IncorporatedBody
      case "05" => UnincorporatedBody
      case _ =>
        ValidationError("error.invalid")
    }
  }

  implicit val jsonWritesBusinessStructure = Writes[BusinessStructure] {
    case SoleProprietor => Json.obj("agentsBusinessStructure" -> "01")
    case LimitedLiabilityPartnership => Json.obj("agentsBusinessStructure" -> "02")
    case Partnership => Json.obj("agentsBusinessStructure" -> "03")
    case IncorporatedBody => Json.obj("agentsBusinessStructure" -> "04")
    case UnincorporatedBody => Json.obj("agentsBusinessStructure" -> "05")
  }

  implicit val agentsBusinessStructureRule: Rule[UrlFormEncoded, BusinessStructure] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._

    (__ \ "agentsBusinessStructure").read[String] flatMap {
      case "01" => SoleProprietor
      case "02" => LimitedLiabilityPartnership
      case "03" => Partnership
      case "04" => IncorporatedBody
      case "05" => UnincorporatedBody
      case _ => (Path \ "agentsBusinessStructure") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWritesBusinessStructure: Write[BusinessStructure, UrlFormEncoded] = Write {
    case SoleProprietor =>
      Map("agentsBusinessStructure" -> Seq("01"))
    case LimitedLiabilityPartnership =>
      Map("agentsBusinessStructure" -> Seq("02"))
    case Partnership =>
      Map("agentsBusinessStructure" -> Seq("03"))
    case IncorporatedBody =>
      Map("agentsBusinessStructure" -> Seq("04"))
    case UnincorporatedBody =>
      Map("agentsBusinessStructure" -> Seq("05"))
  }

}