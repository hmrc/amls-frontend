package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._
import play.api.data.mapping.forms.Rules._
import play.api.libs.functional.syntax._
import utils.MappingUtils.Implicits._

sealed trait IsKnownByOtherNames

case class IsKnownByOtherNamesYes(otherfirstnames: String,
                                  othermiddlenames: Option[String],
                                  otherlastnames: String) extends IsKnownByOtherNames

case object IsKnownByOtherNamesNo extends IsKnownByOtherNames

object IsKnownByOtherNames {

  val maxNameTypeLength = 35
  val firstNameType = notEmpty.withMessage("error.required.firstname") compose
    maxLength(maxNameTypeLength).withMessage("error.invalid.length.firstname")

  implicit val formRule: Rule[UrlFormEncoded, IsKnownByOtherNames] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import utils.MappingUtils.Implicits._
    (__ \ "isKnownByOtherNames").read[Boolean] flatMap {
      case true => (
        (__ \ "otherfirstnames").read(firstNameType) and
        (__ \ "othermiddlenames").read(optionR(firstNameType)) and
        (__ \ "otherlastnames").read(firstNameType)
        )(IsKnownByOtherNamesYes.apply _)
      case false => Rule.fromMapping { _ => Success(IsKnownByOtherNamesNo) }
    }
  }

  implicit val formWrites: Write[IsKnownByOtherNames, UrlFormEncoded] = Write {
    case a: IsKnownByOtherNamesYes => Map(
      "isKnownByOtherNames" -> Seq("true"),
      "otherfirstnames" -> Seq(a.otherfirstnames),
      "othermiddlenames" -> Seq(a.othermiddlenames.getOrElse("")),
      "otherlastnames" -> Seq(a.otherlastnames)
    )
    case IsKnownByOtherNamesNo => Map("isKnownByOtherNames" -> Seq("false"))
  }

  implicit val jsonReads: Reads[IsKnownByOtherNames] =
    (__ \ "isKnownByOtherNames").read[Boolean] flatMap {
      case true => (
        (__ \ "otherfirstnames").read[String] and
          (__ \ "othermiddlenames").readNullable[String] and
          (__ \ "otherlastnames").read[String]
        ) (IsKnownByOtherNamesYes.apply _)
      case false => Reads(_ => JsSuccess(IsKnownByOtherNamesNo))
    }

  implicit val jsonWrites = Writes[IsKnownByOtherNames] {
    case a : IsKnownByOtherNamesYes => Json.obj(
      "isKnownByOtherNames" -> true,
      "otherfirstnames" -> a.otherfirstnames,
      "othermiddlenames" -> a.othermiddlenames,
      "otherlastnames" -> a.otherlastnames
    )
    case IsKnownByOtherNamesNo => Json.obj("isKnownByOtherNames" -> false)
  }

}


