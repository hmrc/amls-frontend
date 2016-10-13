package models.asp

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._
import play.api.data.mapping.forms.UrlFormEncoded

sealed trait OtherBusinessTaxMatters

case object OtherBusinessTaxMattersYes extends OtherBusinessTaxMatters

case object OtherBusinessTaxMattersNo extends OtherBusinessTaxMatters

object OtherBusinessTaxMatters {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, OtherBusinessTaxMatters] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "otherBusinessTaxMatters").read[Boolean].withMessage("error.required.asp.other.business.tax.matters") flatMap {
      case true => Rule.fromMapping { _ => Success(OtherBusinessTaxMattersYes) }
      case false => Rule.fromMapping { _ => Success(OtherBusinessTaxMattersNo) }
    }
  }

  implicit val formWrites: Write[OtherBusinessTaxMatters, UrlFormEncoded] = Write {
    case OtherBusinessTaxMattersYes => Map("otherBusinessTaxMatters" -> Seq("true"))
    case OtherBusinessTaxMattersNo => Map("otherBusinessTaxMatters" -> Seq("false"))
  }

  implicit val jsonReads: Reads[OtherBusinessTaxMatters] =
    (__ \ "otherBusinessTaxMatters").read[Boolean] flatMap {
      case true => Reads(__ => JsSuccess(OtherBusinessTaxMattersYes))
      case false => Reads(__ => JsSuccess(OtherBusinessTaxMattersNo))
    }

  implicit val jsonWrites = Writes[OtherBusinessTaxMatters] {
    case OtherBusinessTaxMattersYes => Json.obj("otherBusinessTaxMatters" -> true)
    case OtherBusinessTaxMattersNo => Json.obj("otherBusinessTaxMatters" -> false)
  }
}
