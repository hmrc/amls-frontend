package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._
import play.api.data.mapping.forms.Rules._
import utils.MappingUtils.Implicits._

sealed trait SaRegistered

case class SaRegisteredYes(value: String) extends SaRegistered

case object SaRegisteredNo extends SaRegistered

object SaRegistered {

  val utrTypeRegex = "^[0-9]{10}$".r
  val utrType = notEmpty.withMessage("error.required.utr.number") compose pattern(utrTypeRegex).withMessage("error.invalid.length.utr.number")

  implicit val formRule: Rule[UrlFormEncoded, SaRegistered] = From[UrlFormEncoded] { __ =>
  import play.api.data.mapping.forms.Rules._
    (__ \ "saRegistered").read[Boolean].withMessage("error.required.sa.registration") flatMap {
      case true =>
        (__ \ "utrNumber").read(utrType) fmap (SaRegisteredYes.apply)
      case false => Rule.fromMapping { _ => Success(SaRegisteredNo) }
    }
  }

  implicit val formWrites: Write[SaRegistered, UrlFormEncoded] = Write {
    case SaRegisteredYes(value) =>
      Map("saRegistered" -> Seq("true"),
          "utrNumber" -> Seq(value)
      )
    case SaRegisteredNo => Map("saRegistered" -> Seq("false"))
  }

  implicit val jsonReads: Reads[SaRegistered] =
    (__ \ "saRegistered").read[Boolean] flatMap {
      case true => (__ \ "utrNumber").read[String] map (SaRegisteredYes.apply _)
      case false => Reads(_ => JsSuccess(SaRegisteredNo))
    }

  implicit val jsonWrites = Writes[SaRegistered] {
    case SaRegisteredYes(value) => Json.obj(
      "saRegistered" -> true,
      "utrNumber" -> value
    )
    case SaRegisteredNo => Json.obj("saRegistered" -> false)
  }

}


