package models.aboutthebusiness

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait VATRegistered

case class VATRegisteredYes(value : String) extends VATRegistered
case object VATRegisteredNo extends VATRegistered


object VATRegistered {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, VATRegistered] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "registeredForVAT").read[Option[Boolean]] flatMap {
      case Some(true) =>
        (__ \ "vrnNumber").read(vrnType) fmap VATRegisteredYes.apply
      case Some(false) => Rule.fromMapping { _ => Success(VATRegisteredNo) }
      case _ => (Path \ "registeredForVAT") -> Seq(ValidationError("error.required.atb.registered.for.vat"))
    }
  }

  implicit val formWrites: Write[VATRegistered, UrlFormEncoded] = Write {
    case VATRegisteredYes(value) =>
      Map("registeredForVAT" -> Seq("true"),
        "vrnNumber" -> Seq(value)
      )
    case VATRegisteredNo => Map("registeredForVAT" -> Seq("false"))
  }

  implicit val jsonReads: Reads[VATRegistered] =
    (__ \ "registeredForVAT").read[Boolean] flatMap {
    case true => (__ \ "vrnNumber").read[String] map (VATRegisteredYes.apply _)
    case false => Reads(_ => JsSuccess(VATRegisteredNo))
  }

  implicit val jsonWrites = Writes[VATRegistered] {
    case VATRegisteredYes(value) => Json.obj(
      "registeredForVAT" -> true,
      "vrnNumber" -> value
    )
    case VATRegisteredNo => Json.obj("registeredForVAT" -> false)
  }
}