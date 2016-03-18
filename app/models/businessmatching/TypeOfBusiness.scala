package models.businessmatching

import models.FormTypes._
import play.api.data.mapping.{Write, From, Rule}
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.Json

case class TypeOfBusiness(typeOfBusiness: String)

object TypeOfBusiness{

  implicit val format = Json.format[TypeOfBusiness]

  implicit val formRead:Rule[UrlFormEncoded, TypeOfBusiness] = From[UrlFormEncoded] {__ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "typeOfBusiness").read(typeOfBusinessType) fmap TypeOfBusiness.apply
  }

  implicit val formWrite: Write[TypeOfBusiness, UrlFormEncoded] = Write {
    case TypeOfBusiness(p) => Map("typeOfBusiness" -> Seq(p.toString))
  }

}

