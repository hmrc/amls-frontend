package models.aboutthebusiness

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait RegisteredForVAT

case class RegisteredForVATYes(value : String) extends RegisteredForVAT
case object RegisteredForVATNo extends RegisteredForVAT


object RegisteredForVAT {

  implicit val formRule: Rule[UrlFormEncoded, RegisteredForVAT] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "registeredForVAT").read[Boolean] flatMap {

      case true =>
        (__ \ "registeredForVATYes").read(minLength(1)) flatMap {
          value =>
            Rule.fromMapping { _ => Success(RegisteredForVATYes(value)) }
        }
      case false => Rule.fromMapping { _ => Success(RegisteredForVATNo) }
    }
  }

  implicit val formWrites: Write[RegisteredForVAT, UrlFormEncoded] = Write {
    case RegisteredForVATYes(value) =>
      Map("registeredForVAT" -> Seq("true"),
        "registeredForVATYes" -> Seq(value)
      )
    case RegisteredForVATNo => Map("registeredForVAT" -> Seq("false"))
  }

  implicit val jsonReads =
    (__ \ "registeredForVAT").read[Boolean] flatMap[RegisteredForVAT] {
    case true => (__ \ "registeredForVATYes").read[String] map {
      RegisteredForVATYes(_)
    }
    case false => Reads(_ => JsSuccess(RegisteredForVATNo))
  }

  implicit val jsonWrites = Writes[RegisteredForVAT] {
    case RegisteredForVATYes(value) => Json.obj(
      "registeredForVAT" -> true,
      "registeredForVATYes" -> value
    )
    case RegisteredForVATNo => Json.obj("registeredForVAT" -> false)
  }
}