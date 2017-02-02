package models.asp

import jto.validation._
import jto.validation.forms.Rules._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._
import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.UrlFormEncoded

sealed trait OtherBusinessTaxMatters

case object OtherBusinessTaxMattersYes extends OtherBusinessTaxMatters

case object OtherBusinessTaxMattersNo extends OtherBusinessTaxMatters

object OtherBusinessTaxMatters {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, OtherBusinessTaxMatters] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "otherBusinessTaxMatters").read[Boolean].withMessage("error.required.asp.other.business.tax.matters") flatMap {
      case true => Rule.fromMapping { _ => Valid(OtherBusinessTaxMattersYes) }
      case false => Rule.fromMapping { _ => Valid(OtherBusinessTaxMattersNo) }
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
