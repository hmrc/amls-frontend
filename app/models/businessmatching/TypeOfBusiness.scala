package models.businessmatching

import models.FormTypes._
import jto.validation.{Write, From, Rule}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Json

case class TypeOfBusiness(typeOfBusiness: String)

object TypeOfBusiness{

  implicit val format = Json.format[TypeOfBusiness]

  implicit val formRead:Rule[UrlFormEncoded, TypeOfBusiness] = From[UrlFormEncoded] {__ =>
    import jto.validation.forms.Rules._
    (__ \ "typeOfBusiness").read(typeOfBusinessType) fmap TypeOfBusiness.apply
  }

  implicit val formWrite: Write[TypeOfBusiness, UrlFormEncoded] = Write {
    case TypeOfBusiness(p) => Map("typeOfBusiness" -> Seq(p.toString))
  }

}

