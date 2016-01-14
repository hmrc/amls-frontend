package models.aboutthebusiness

import models.aboutthebusiness
import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class RegOfficeOrMainPlaceOfBusiness (isRegOfficeOrMainPlaceOfBusiness: Boolean )

object RegOfficeOrMainPlaceOfBusiness {

  implicit val formats = Json.format[RegOfficeOrMainPlaceOfBusiness]

  implicit val formRule: Rule[UrlFormEncoded, RegOfficeOrMainPlaceOfBusiness] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "isRegOfficeOrMainPlaceOfBusiness").read[Boolean].fmap (RegOfficeOrMainPlaceOfBusiness.apply)
  }

  implicit val formWrites: Write[RegOfficeOrMainPlaceOfBusiness, UrlFormEncoded] = Write {
    case RegOfficeOrMainPlaceOfBusiness(b) => Map("isRegOfficeOrMainPlaceOfBusiness" -> Seq(b.toString))
  }
}