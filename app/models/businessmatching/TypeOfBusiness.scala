package models.businessmatching

import play.api.data.mapping.{From, Rule}
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.Json

case class TypeOfBusiness(typeOfBusiness: String)

object TypeOfBusiness{
    implicit val format = Json.format[TypeOfBusiness]

  implicit val formRead:Rule[UrlFormEncoded, TypeOfBusiness] = From[UrlFormEncoded] {
    (__ \ "businessType").read[String]
  }

}

