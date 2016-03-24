package models.aboutthebusiness

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait PreviouslyRegistered

case class PreviouslyRegisteredYes(value: String) extends PreviouslyRegistered

case object PreviouslyRegisteredNo extends PreviouslyRegistered

object PreviouslyRegistered {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PreviouslyRegistered] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "previouslyRegistered").read[Option[Boolean]] flatMap {
      case Some(true) =>
        (__ \ "prevMLRRegNo").read(customNotEmpty("error.required.atb.mlr.number")
          compose customRegex("^([0-9]{8}|[0-9]{15})$".r, "error.invalid.atb.mlr.number")) fmap PreviouslyRegisteredYes.apply
      case Some(false) => Rule.fromMapping { _ => Success(PreviouslyRegisteredNo) }
      case _ => (Path \ "previouslyRegistered") -> Seq(ValidationError("error.required.atb.previously.registered"))
    }
  }

  implicit val formWrites: Write[PreviouslyRegistered, UrlFormEncoded] = Write {
    case PreviouslyRegisteredYes(value) =>
      Map("previouslyRegistered" -> Seq("true"),
        "prevMLRRegNo" -> Seq(value)
      )
    case PreviouslyRegisteredNo => Map("previouslyRegistered" -> Seq("false"))
  }

  implicit val jsonReads: Reads[PreviouslyRegistered] =
    (__ \ "previouslyRegistered").read[Boolean] flatMap {
      case true => (__ \ "prevMLRRegNo").read[String] map PreviouslyRegisteredYes.apply
      case false => Reads(_ => JsSuccess(PreviouslyRegisteredNo))
    }

  implicit val jsonWrites = Writes[PreviouslyRegistered] {
    case PreviouslyRegisteredYes(value) => Json.obj(
      "previouslyRegistered" -> true,
      "prevMLRRegNo" -> value
    )
    case PreviouslyRegisteredNo => Json.obj("previouslyRegistered" -> false)
  }
}