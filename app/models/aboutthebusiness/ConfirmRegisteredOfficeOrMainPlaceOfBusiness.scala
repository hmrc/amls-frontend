package models.aboutthebusiness

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class ConfirmRegisteredOfficeOrMainPlaceOfBusiness (isRegOfficeOrMainPlaceOfBusiness: Boolean )

object ConfirmRegisteredOfficeOrMainPlaceOfBusiness {

  implicit val formats = Json.format[ConfirmRegisteredOfficeOrMainPlaceOfBusiness]

  implicit val formRule: Rule[UrlFormEncoded, ConfirmRegisteredOfficeOrMainPlaceOfBusiness] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "isRegOfficeOrMainPlaceOfBusiness").read[Boolean].fmap (ConfirmRegisteredOfficeOrMainPlaceOfBusiness.apply)
  }

  implicit val formWrites: Write[ConfirmRegisteredOfficeOrMainPlaceOfBusiness, UrlFormEncoded] = Write {
    case ConfirmRegisteredOfficeOrMainPlaceOfBusiness(b) => Map("isRegOfficeOrMainPlaceOfBusiness" -> Seq(b.toString))
  }
}