package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._
import play.api.data.mapping.forms.Rules._
import utils.MappingUtils.Implicits._

sealed trait IsKnownByOtherNames

case class IsKnownByOtherNamesYes(value: String) extends IsKnownByOtherNames

case object IsKnownByOtherNamesNo extends IsKnownByOtherNames

object IsKnownByOtherNames {

  val maxNameTypeLength = 35
  val firstNameType = notEmpty.withMessage("error.required.firstname") compose
    maxLength(maxNameTypeLength).withMessage("error.invalid.length.firstname")

  implicit val formRule: Rule[UrlFormEncoded, IsKnownByOtherNames] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "isKnownByOtherNames").read[Boolean] flatMap {
      case true => (__ \ "otherfirstnames").read(firstNameType) fmap (IsKnownByOtherNamesYes.apply)
      case false => Rule.fromMapping { _ => Success(IsKnownByOtherNamesNo) }
    }
  }

  implicit val formWrites: Write[IsKnownByOtherNames, UrlFormEncoded] = Write {
    case IsKnownByOtherNamesYes(value) =>
      Map("isKnownByOtherNames" -> Seq("true"),
        "otherfirstnames" -> Seq(value)
      )
    case IsKnownByOtherNamesNo => Map("isKnownByOtherNames" -> Seq("false"))
  }

  implicit val jsonReads: Reads[IsKnownByOtherNames] =
    (__ \ "isKnownByOtherNames").read[Boolean] flatMap {
      case true => (__ \ "otherfirstnames").read[String] map (IsKnownByOtherNamesYes.apply _)
      case false => Reads(_ => JsSuccess(IsKnownByOtherNamesNo))
    }

  implicit val jsonWrites = Writes[IsKnownByOtherNames] {
    case IsKnownByOtherNamesYes(value) => Json.obj(
      "isKnownByOtherNames" -> true,
      "otherfirstnames" -> value
    )
    case IsKnownByOtherNamesNo => Json.obj("isKnownByOtherNames" -> false)
  }

}


