package models.businessactivities

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class TaxMatters(manageYourTaxAffairs: Boolean)

object TaxMatters {

  implicit val formats = Json.format[TaxMatters]

  implicit val formRule: Rule[UrlFormEncoded, TaxMatters] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      import utils.MappingUtils.Implicits._
      (__ \ "manageYourTaxAffairs").read[Boolean].withMessage("error.required.ba.tax.matters") fmap TaxMatters.apply
    }

  implicit val formWrites: Write[TaxMatters, UrlFormEncoded] =
    Write {
      case TaxMatters(b) =>
        Map("manageYourTaxAffairs" -> Seq(b.toString))
    }
}
