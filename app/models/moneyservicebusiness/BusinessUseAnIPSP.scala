package models.moneyservicebusiness

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait BusinessUseAnIPSP

case class BusinessUseAnIPSPYes(name: String , reference: String) extends BusinessUseAnIPSP

case object BusinessUseAnIPSPNo extends BusinessUseAnIPSP

object BusinessUseAnIPSP {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  private val maxNameTypeLength = 140
  private val nameType = notEmptyStrip compose
    notEmpty.withMessage("error.required.msb.ipsp.name") compose
    maxLength(maxNameTypeLength).withMessage("error.invalid.msb.ipsp.name")

  val referenceTypeRegex = "^[0-9]{15}$".r
  private val referenceNumRegex = regexWithMsg(referenceTypeRegex, "error.required.msb.ipsp.reference")

  val referenceType = notEmptyStrip compose
    notEmpty.withMessage("error.required.msb.ipsp.reference") compose referenceNumRegex


  implicit val formRule: Rule[UrlFormEncoded, BusinessUseAnIPSP] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "useAnIPSP").read[Boolean].withMessage("error.required.msb.ipsp") flatMap {
      case true =>
        ((__ \ "name").read(nameType) and
          (__ \ "referenceNumber").read(referenceType)) (BusinessUseAnIPSPYes.apply _)
      case false => Rule.fromMapping { _ => Success(BusinessUseAnIPSPNo) }
    }
  }

  implicit val formWrites: Write[BusinessUseAnIPSP, UrlFormEncoded] = Write {
    case BusinessUseAnIPSPYes(name, number) =>
      Map("useAnIPSP" -> Seq("true"),
        "name" -> Seq(name),
        "referenceNumber" -> Seq(number)
      )
    case BusinessUseAnIPSPNo => Map("useAnIPSP" -> Seq("false"))
  }

  implicit val jsonReads: Reads[BusinessUseAnIPSP] = {
    import play.api.libs.functional.syntax._
    (__ \ "useAnIPSP").read[Boolean] flatMap {
      case true => ((__ \ "name").read[String] and
        (__ \ "referenceNumber").read[String]) (BusinessUseAnIPSPYes.apply _)
      case false => Reads(_ => JsSuccess(BusinessUseAnIPSPNo))
    }
  }

  implicit val jsonWrites = Writes[BusinessUseAnIPSP] {
    case BusinessUseAnIPSPYes(name ,referenceNumber) => Json.obj(
                                          "useAnIPSP" -> true,
                                           "name" -> name,
                                           "referenceNumber" -> referenceNumber

                                        )
    case BusinessUseAnIPSPNo => Json.obj("useAnIPSP" -> false)
  }

}
