package models.tcsp

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import cats.data.Validated.{Invalid, Valid}

sealed trait ServicesOfAnotherTCSP

case class ServicesOfAnotherTCSPYes(mlrRefNumber: String) extends ServicesOfAnotherTCSP

case object ServicesOfAnotherTCSPNo extends ServicesOfAnotherTCSP

object ServicesOfAnotherTCSP {

  import utils.MappingUtils.Implicits._

  private val mlrPattern = "^([0-9]{8}|[0-9]{15})$".r

  val service = notEmpty
    .withMessage("error.required.tcsp.mlr.reference.number")
    .andThen(pattern(mlrPattern).withMessage("error.invalid.tcsp.mlr.reference.number"))

  implicit val formRule: Rule[UrlFormEncoded, ServicesOfAnotherTCSP] = From[UrlFormEncoded] { __ =>
  import jto.validation.forms.Rules._
    (__ \ "servicesOfAnotherTCSP").read[Boolean].withMessage("error.required.tcsp.services.another.tcsp") flatMap {
      case true =>
       (__ \ "mlrRefNumber").read(service) map ServicesOfAnotherTCSPYes.apply
      case false => Rule.fromMapping { _ => Valid(ServicesOfAnotherTCSPNo) }
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
