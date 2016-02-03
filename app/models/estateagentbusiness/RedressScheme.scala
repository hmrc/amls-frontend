package models.estateagentbusiness


import play.api.data.validation.ValidationError
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait RedressRegistered

case class RedressRegisteredYes(value : RedressScheme) extends RedressRegistered
case object RedressRegisteredNo extends RedressRegistered

sealed trait RedressScheme

case object ThePropertyOmbudsman extends RedressScheme
case object OmbudsmanServices extends RedressScheme
case object PropertyRedressScheme extends RedressScheme
case class Other(v: String) extends RedressScheme


object RedressRegistered {
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, RedressRegistered] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (__ \ "isRedress").read[Boolean] flatMap {
      case true => __.read[RedressScheme] fmap RedressRegisteredYes.apply
      case false => Rule.fromMapping { _ => Success(RedressRegisteredNo) }
    }
  }

  implicit val formWrites: Write[RedressRegistered, UrlFormEncoded] = Write {
    case RedressRegisteredYes(redress) =>
      redress match {
        case ThePropertyOmbudsman =>Map("isRedress" -> "true","propertyRedressScheme" -> "01")
        case OmbudsmanServices => Map("isRedress" -> "true","propertyRedressScheme" -> "02")
        case PropertyRedressScheme => Map("isRedress" -> "true","propertyRedressScheme" -> "03")
        case Other(value) =>
          Map ("isRedress" -> "true",
            "propertyRedressScheme" -> "04",
            "other" -> value)
      }
    case RedressRegisteredNo => Map("propertyRedressScheme" -> Seq("false"))
  }

  implicit val jsonReads: Reads[RedressRegistered] =
    (__ \ "isRedress").read[Boolean] flatMap {
      case true => __.read[RedressScheme] map RedressRegisteredYes.apply
      case false => Reads(_ => JsSuccess(RedressRegisteredNo))
    }

  implicit val jsonWrites = Writes[RedressRegistered] {
    case RedressRegisteredYes(value) => value match {
      case ThePropertyOmbudsman => Json.obj("isRedress" -> true,"propertyRedressScheme" -> "01")
      case OmbudsmanServices => Json.obj("isRedress" -> true,"propertyRedressScheme" -> "01")
      case PropertyRedressScheme => Json.obj("isRedress" -> true,"propertyRedressScheme" -> "03")
      case Other(value) =>
        Json.obj(
          "isRedress" -> true,
          "propertyRedressScheme" -> "04",
          "propertyRedressSchemeOther" -> value
        )
    }
    case RedressRegisteredNo => Json.obj("isRedress" -> false)
  }
}

object RedressScheme {
  import utils.MappingUtils.Implicits._

  implicit val formRedressRule: Rule[UrlFormEncoded, RedressScheme] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    ( __ \ "propertyRedressScheme").read[String] flatMap {
      case "01" => ThePropertyOmbudsman
      case "02" => OmbudsmanServices
      case "03" => PropertyRedressScheme
      case "04" =>
        (__ \ "other").read(descriptionType) fmap Other.apply
      case _ =>
        (Path \ "propertyRedressScheme") -> Seq(ValidationError("error.invalid"))
    }
  }

/*
  implicit val formRedressWrites: Write[RedressScheme, UrlFormEncoded] = Write {
    case ThePropertyOmbudsman => "propertyRedressScheme" -> "01"
    case OmbudsmanServices => "propertyRedressScheme" -> "02"
    case PropertyRedressScheme => "propertyRedressScheme" -> "03"
    case Other(value) =>
      Map(
        "propertyRedressScheme" -> "08",
        "other" -> value
      )
  }
*/
  implicit val jsonRedressReads : Reads[RedressScheme] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "propertyRedressScheme").read[String].flatMap[RedressScheme] {
      case "01" => ThePropertyOmbudsman
      case "02" => OmbudsmanServices
      case "03" => PropertyRedressScheme
      case "04" =>
        (JsPath \ "propertyRedressSchemeOther").read[String] map {
          Other(_)
        }
      case _ =>
        ValidationError("error.invalid")
    }
  }
}


