package models.businessmatching

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import play.api.libs.json._
import models.FormTypes._

case class TypeOfBusiness(typeOfBusiness: String)

object TypeOfBusiness{


  implicit val format = Json.format[TypeOfBusiness]

  implicit val formRead:Rule[UrlFormEncoded, TypeOfBusiness] = From[UrlFormEncoded] {__ =>
    import jto.validation.forms.Rules._

    val maxTypeOfBusinessLength = 40
    val typeOfBusinessLength = maxWithMsg(maxTypeOfBusinessLength, "error.max.length.bm.businesstype.type")
    val typeOfBusinessRequired = required("error.required.bm.businesstype.type")
    val typeOfBusinessType = notEmptyStrip andThen typeOfBusinessRequired andThen typeOfBusinessLength andThen basicPunctuationPattern

    (__ \ "typeOfBusiness").read(typeOfBusinessType) map TypeOfBusiness.apply
  }

  implicit val formWrite: Write[TypeOfBusiness, UrlFormEncoded] = Write {
    case TypeOfBusiness(p) => Map("typeOfBusiness" -> Seq(p.toString))
  }

}
