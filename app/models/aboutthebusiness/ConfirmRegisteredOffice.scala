package models.aboutthebusiness

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class ConfirmRegisteredOffice(isRegOfficeOrMainPlaceOfBusiness: Boolean)

object ConfirmRegisteredOffice {

  implicit val formats = Json.format[ConfirmRegisteredOffice]

  implicit val formRule: Rule[UrlFormEncoded, ConfirmRegisteredOffice] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (__ \ "isRegOfficeOrMainPlaceOfBusiness").read[Boolean] fmap (ConfirmRegisteredOffice.apply)
    }

  implicit val formWrites: Write[ConfirmRegisteredOffice, UrlFormEncoded] =
    Write {
      case ConfirmRegisteredOffice(b) =>
        Map("isRegOfficeOrMainPlaceOfBusiness" -> Seq(b.toString))
    }
}