package models.aboutthebusiness

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait PreviouslyRegistered

case class PreviouslyRegisteredYes(value : String) extends PreviouslyRegistered
case object PreviouslyRegisteredNo extends PreviouslyRegistered

object PreviouslyRegistered {

  import models.FormTypes._

  implicit val formRule: Rule[UrlFormEncoded, PreviouslyRegistered] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "previouslyRegistered").read[Boolean] flatMap {
      case true =>
        (__ \ "prevMLRRegNo").read(prevMLRRegNoType) fmap (PreviouslyRegisteredYes.apply _)
      case false => Rule.fromMapping { _ => Success(PreviouslyRegisteredNo) }
    }
  }

  implicit val formWrites: Write[PreviouslyRegistered, UrlFormEncoded] = Write {
    case PreviouslyRegisteredYes(value) =>
      Map("previouslyRegistered" -> Seq("true"),
        "prevMLRRegNo" -> Seq(value)
      )
    case PreviouslyRegisteredNo => Map("previouslyRegistered" -> Seq("false"))
  }

  implicit val jsonReads =
    (__ \ "previouslyRegistered").read[Boolean] flatMap[PreviouslyRegistered] {
    case true => (__ \ "prevMLRRegNo").read[String] map (PreviouslyRegisteredYes.apply _)
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