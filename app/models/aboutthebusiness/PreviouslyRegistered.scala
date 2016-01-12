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
    (__ \ "previouslyRegistered").read[String] flatMap {

      case "01" =>
        (__ \ "previouslyRegisteredYes").read(minLength(1)) flatMap {
          value =>
            Rule.fromMapping { _ => Success(PreviouslyRegisteredYes(value)) }
        }
      case "02" => Rule.fromMapping { _ => Success(PreviouslyRegisteredNo) }
      case _ => Rule { _ =>
        Failure(Seq((Path \ "previouslyRegistered") -> Seq(ValidationError("error.invalid"))))
      }
    }
  }

  implicit val formWrites: Write[PreviouslyRegistered, UrlFormEncoded] = Write {
    case PreviouslyRegisteredYes(value) =>
      Map("previouslyRegistered" -> Seq("01"),
        "previouslyRegisteredYes" -> Seq(value)
      )
    case PreviouslyRegisteredNo => Map("previouslyRegistered" -> Seq("02"))
  }

  implicit val jsonReads =
    (__ \ "previouslyRegistered").read[String] flatMap[PreviouslyRegistered] {
    case "01" => (__ \ "previouslyRegisteredYes").read[String] map {
      PreviouslyRegisteredYes(_)
    }
    case "02" => Reads(_ => JsSuccess(PreviouslyRegisteredNo))
  }

  implicit val jsonWrites = Writes[PreviouslyRegistered] {
    case PreviouslyRegisteredYes(value) => Json.obj(
      "previouslyRegistered" -> "01",
      "previouslyRegisteredYes" -> value
    )
    case PreviouslyRegisteredNo => Json.obj("previouslyRegistered" -> "02")
  }
}