package models.aboutthebusiness

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait PreviouslyRegistered

case class PreviouslyRegisteredYes(value : String) extends PreviouslyRegistered
case object PreviouslyRegisteredNo extends PreviouslyRegistered


object PreviouslyRegistered {

  implicit val formRule: Rule[UrlFormEncoded, PreviouslyRegistered] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "previouslyRegistered").read[Boolean] flatMap {

      case true =>
        (__ \ "previouslyRegisteredYes").read(minLength(1)) flatMap {
          value =>
            Rule.fromMapping { _ => Success(PreviouslyRegisteredYes(value)) }
        }
      case false => Rule.fromMapping { _ => Success(PreviouslyRegisteredNo) }
    }
  }

  implicit val formWrites: Write[PreviouslyRegistered, UrlFormEncoded] = Write {
    case PreviouslyRegisteredYes(value) =>
      Map("previouslyRegistered" -> Seq("true"),
        "previouslyRegisteredYes" -> Seq(value)
      )
    case PreviouslyRegisteredNo => Map("previouslyRegistered" -> Seq("false"))
  }

  implicit val jsonReads =
    (__ \ "previouslyRegistered").read[Boolean] flatMap[PreviouslyRegistered] {
    case true => (__ \ "previouslyRegisteredYes").read[String] map {
      PreviouslyRegisteredYes(_)
    }
    case false => Reads(_ => JsSuccess(PreviouslyRegisteredNo))
  }

  implicit val jsonWrites = Writes[PreviouslyRegistered] {
    case PreviouslyRegisteredYes(value) => Json.obj(
      "previouslyRegistered" -> true,
      "previouslyRegisteredYes" -> value
    )
    case PreviouslyRegisteredNo => Json.obj("previouslyRegistered" -> false)
  }
}