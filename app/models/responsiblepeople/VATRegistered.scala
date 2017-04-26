package models.responsiblepeople

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait VATRegistered

case class VATRegisteredYes(value : String) extends VATRegistered
case object VATRegisteredNo extends VATRegistered


object VATRegistered {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._
  import cats.data.Validated.{Invalid, Valid}
  implicit val formRule: Rule[UrlFormEncoded, VATRegistered] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "registeredForVAT").read[Boolean].withMessage("error.required.rp.registered.for.vat") flatMap {
      case true =>
        (__ \ "vrnNumber").read(vrnType) map VATRegisteredYes.apply
      case false => Rule.fromMapping { _ => Valid(VATRegisteredNo) }
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
    case true => (__ \ "vrnNumber").read[String] map VATRegisteredYes
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
