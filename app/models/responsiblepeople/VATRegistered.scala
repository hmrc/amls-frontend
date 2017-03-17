package models.responsiblepeople

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait VATRegistered

case class VATRegisteredYes(value: String) extends VATRegistered

case object VATRegisteredNo extends VATRegistered


object VATRegistered {

  import cats.data.Validated.{Invalid, Valid}
  import models.FormTypes._
  import utils.MappingUtils.Implicits._


  private val responseRequiredMapping = Rule.fromMapping[(String, Option[Boolean], Option[String]), (String, Boolean, Option[String])] {
    case (name, None, _) => Invalid(Seq(ValidationError("error.required.atb.registered.for.vat", name)))
    case (name, Some(response), number) => Valid((name, response, number))
  }

  private val vatNumberRequiredMapping = Rule.fromMapping[(String, Boolean, Option[String]), VATRegistered] {
    case (_, true, Some(number)) => Valid(VATRegisteredYes(number))
    case (_, true, None) => Invalid(Seq(ValidationError("error.required.vat.number")))
    case (_, false, None) => Valid(VATRegisteredNo)
  }

  implicit val formRule: Rule[UrlFormEncoded, VATRegistered] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    ((__ \ "personName").read[String] ~
      (__ \ "registeredForVAT").read[Option[Boolean]] ~
      (__ \ "vrnNumber").read(optionR(vrnType))
      ).tupled
      .andThen(responseRequiredMapping.repath(_ => Path \ "registeredForVAT"))
      .andThen(vatNumberRequiredMapping.repath(_ => Path \ "vrnNumber"))
  }

  implicit val formWrites: Write[VATRegistered, UrlFormEncoded] = Write {
    case VATRegisteredYes(value) =>
      Map("registeredForVAT" -> Seq("true"),
        "vrnNumber" -> Seq(value)
      )
    case VATRegisteredNo => Map("registeredForVAT" -> Seq("false"))
  }

  implicit val jsonReads: Reads[VATRegistered] =
    (__ \ "registeredForVAT").read[Boolean] flatMap {
      case true => (__ \ "vrnNumber").read[String] map (VATRegisteredYes.apply _)
      case false => Reads(_ => JsSuccess(VATRegisteredNo))
    }

  implicit val jsonWrites = Writes[VATRegistered] {
    case VATRegisteredYes(value) => Json.obj(
      "registeredForVAT" -> true,
      "vrnNumber" -> value
    )
    case VATRegisteredNo => Json.obj("registeredForVAT" -> false)
  }
}
