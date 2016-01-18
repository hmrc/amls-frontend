package models.aboutthebusiness

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait VATRegistered

case class VATRegisteredYes(value : String) extends VATRegistered
case object VATRegisteredNo extends VATRegistered


object VATRegistered {

  import models.FormTypes._

  implicit val formRule: Rule[UrlFormEncoded, VATRegistered] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "registeredForVAT").read[Boolean] flatMap {
      case true =>
        (__ \ "vrnNumber").read(vrnType) fmap (VATRegisteredYes.apply)
      case false => Rule.fromMapping { _ => Success(VATRegisteredNo) }
    }
  }

  implicit val formWrites: Write[VATRegistered, UrlFormEncoded] = Write {
    case VATRegisteredYes(value) =>
      Map("registeredForVAT" -> Seq("true"),
        "vrnNumber" -> Seq(value)
      )
    case VATRegisteredNo => Map("registeredForVAT" -> Seq("false"))
  }

  implicit val jsonReads =
    (__ \ "registeredForVAT").read[Boolean] flatMap[VATRegistered] {
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