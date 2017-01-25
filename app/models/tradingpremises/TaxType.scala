package models.tradingpremises

import play.api.data.mapping.{Write, Path, From, Rule}
import play.api.data.mapping.forms._
import play.api.data.validation.ValidationError
import play.api.i18n.{Messages, Lang}
import play.api.libs.json.Writes
import play.api.libs.json._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

sealed trait TaxType {
  def message(implicit lang: Lang): String =
    this match {
      case TaxTypeSelfAssesment =>
        Messages("tradingpremises.youragent.taxtype.lbl.01")
      case TaxTypeCorporationTax =>
        Messages("tradingpremises.youragent.taxtype.lbl.02")
    }
}

case object TaxTypeSelfAssesment extends TaxType
case object TaxTypeCorporationTax extends TaxType

object TaxType {

  import utils.MappingUtils.Implicits._

  implicit val jsonReadsTaxType = {
    (__ \ "taxType").read[String].flatMap[TaxType] {
      case "01" => TaxTypeSelfAssesment
      case "02" => TaxTypeCorporationTax
      case _ => ValidationError("error.invalid")
    }
  }

  implicit val jsonWritesTaxType = Writes[TaxType] {
    case TaxTypeSelfAssesment => Json.obj("taxType" -> "01")
    case TaxTypeCorporationTax => Json.obj("taxType" -> "02")
  }

  implicit val taxTypeRule: Rule[UrlFormEncoded, TaxType] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "taxType").read[String] flatMap {
      case "01" => TaxTypeSelfAssesment
      case "02" => TaxTypeCorporationTax
      case _ =>
        (Path \ "taxType") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWritesTaxType: Write[TaxType, UrlFormEncoded] = Write {
    case TaxTypeSelfAssesment =>
      Map("taxType" -> Seq("01"))
    case TaxTypeCorporationTax =>
      Map("taxType" -> Seq("02"))
    case _ => Map("" -> Seq(""))
  }
}
