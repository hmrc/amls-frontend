package models.aboutthebusiness

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait CorporationTaxRegistered

case class CorporationTaxRegisteredYes(corporationTaxReference : String) extends CorporationTaxRegistered
case object CorporationTaxRegisteredNo extends CorporationTaxRegistered


object CorporationTaxRegistered {

  import models.FormTypes._

  implicit val formRule: Rule[UrlFormEncoded, CorporationTaxRegistered] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "registeredForCorporationTax").read[Boolean] flatMap {
      case true =>
        (__ \ "corporationTaxReference").read(corporationTaxType) fmap CorporationTaxRegisteredYes.apply
      case false => Rule.fromMapping { _ => Success(CorporationTaxRegisteredNo) }
    }
  }

  implicit val formWrites: Write[CorporationTaxRegistered, UrlFormEncoded] = Write {
    case CorporationTaxRegisteredYes(value) =>
      Map("registeredForCorporationTax" -> Seq("true"),
        "corporationTaxReference" -> Seq(value)
      )
    case CorporationTaxRegisteredNo => Map("registeredForCorporationTax" -> Seq("false"))
  }

  implicit val jsonReads: Reads[CorporationTaxRegistered] =
    (__ \ "registeredForCorporationTax").read[Boolean] flatMap {
      case true => (__ \ "corporationTaxReference").read[String] map (CorporationTaxRegisteredYes.apply _)
      case false => Reads(_ => JsSuccess(CorporationTaxRegisteredNo))
    }

  implicit val jsonWrites = Writes[CorporationTaxRegistered] {
    case CorporationTaxRegisteredYes(value) => Json.obj(
      "registeredForCorporationTax" -> true,
      "corporationTaxReference" -> value
    )
    case CorporationTaxRegisteredNo => Json.obj("registeredForCorporationTax" -> false)
  }
}