package models.tcsp

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait ServicesOfAnotherTCSP

case class ServicesOfAnotherTCSPYes(value: String) extends ServicesOfAnotherTCSP

case object ServicesOfAnotherTCSPNo extends ServicesOfAnotherTCSP

object ServicesOfAnotherTCSP {

  import utils.MappingUtils.Implicits._
  import models.FormTypes._

  implicit val formRule: Rule[UrlFormEncoded, ServicesOfAnotherTCSP] = From[UrlFormEncoded] { __ =>
  import play.api.data.mapping.forms.Rules._
    (__ \ "servicesOfAnotherTCSP").read[Boolean].withMessage("error.required.tcsp.services.another.tcsp") flatMap {
      case true =>
        (__ \ "mlrRefNumber").read(mlrRefNumberPattern) fmap ServicesOfAnotherTCSPYes.apply
      case false => Rule.fromMapping { _ => Success(ServicesOfAnotherTCSPNo) }
    }
  }

  implicit val formWrites: Write[ServicesOfAnotherTCSP, UrlFormEncoded] = Write {
    case ServicesOfAnotherTCSPYes(value) =>
      Map("servicesOfAnotherTCSP" -> Seq("true"),
        "mlrRefNumber" -> Seq(value)
      )
    case ServicesOfAnotherTCSPNo => Map("servicesOfAnotherTCSP" -> Seq("false"))
  }

  implicit val jsonReads: Reads[ServicesOfAnotherTCSP] =
    (__ \ "servicesOfAnotherTCSP").read[Boolean] flatMap {
      case true => (__ \ "mlrRefNumber").read[String] map ServicesOfAnotherTCSPYes.apply
      case false => Reads(__ => JsSuccess(ServicesOfAnotherTCSPNo))
    }

  implicit val jsonWrites = Writes[ServicesOfAnotherTCSP] {
    case ServicesOfAnotherTCSPYes(value) => Json.obj(
          "servicesOfAnotherTCSP" -> true,
          "mlrRefNumber" -> value
    )
    case ServicesOfAnotherTCSPNo => Json.obj("servicesOfAnotherTCSP" -> false)
  }
}

