package models.estateagentbusiness

import jto.validation.forms.Rules._
import jto.validation.ValidationError
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait RedressScheme

case object ThePropertyOmbudsman extends RedressScheme
case object OmbudsmanServices extends RedressScheme
case object PropertyRedressScheme extends RedressScheme
case class Other(v: String) extends RedressScheme

case object RedressSchemedNo extends RedressScheme

object RedressScheme {
  import utils.MappingUtils.Implicits._

  val maxRedressOtherTypeLength = 255
  val redressOtherType = notEmpty.withMessage("error.required.eab.redress.scheme.name") compose
    maxLength(maxRedressOtherTypeLength).withMessage("error.invalid.eab.redress.scheme.name")

  implicit val formRedressRule: Rule[UrlFormEncoded, RedressScheme] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import models.FormTypes._
    (__ \ "isRedress").read[Boolean].withMessage("error.required.eab.redress.scheme") flatMap {
      case true => {
        ( __ \ "propertyRedressScheme").read[String].withMessage("error.required.eab.which.redress.scheme") flatMap {
          case "01" => ThePropertyOmbudsman
          case "02" => OmbudsmanServices
          case "03" => PropertyRedressScheme
          case "04" =>
            (__ \ "other").read(redressOtherType) fmap Other.apply
          case _ =>
            (Path \ "propertyRedressScheme") -> Seq(ValidationError("error.invalid"))
        }
      }
      case false => Rule.fromMapping { _ => Success(RedressSchemedNo) }
    }
  }


  implicit val formRedressWrites: Write[RedressScheme, UrlFormEncoded] = Write {
    case ThePropertyOmbudsman => Map("isRedress" -> "true","propertyRedressScheme" -> "01")
    case OmbudsmanServices => Map("isRedress" -> "true","propertyRedressScheme" -> "02")
    case PropertyRedressScheme => Map("isRedress" -> "true","propertyRedressScheme" -> "03")
    case Other(value) =>
      Map(
        "isRedress" -> "true",
        "propertyRedressScheme" -> "04",
        "other" -> value
      )
    case RedressSchemedNo => Map("isRedress" -> "false")
  }

  implicit val jsonRedressReads : Reads[RedressScheme] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "isRedress").read[Boolean] flatMap {
      case true =>
      {
        (__ \ "propertyRedressScheme").read[String].flatMap[RedressScheme] {
          case "01" => ThePropertyOmbudsman
          case "02" => OmbudsmanServices
          case "03" => PropertyRedressScheme
          case "04" =>
            (JsPath \ "propertyRedressSchemeOther").read[String] map {
              Other(_)
            }
          case _ =>
            play.api.data.validation.ValidationError("error.invalid")
        }
      }
      case false => Reads(_ => JsSuccess(RedressSchemedNo))
    }
  }

  implicit val jsonRedressWrites = Writes[RedressScheme] {
      case ThePropertyOmbudsman => Json.obj("isRedress" -> true,"propertyRedressScheme" -> "01")
      case OmbudsmanServices => Json.obj("isRedress" -> true,"propertyRedressScheme" -> "02")
      case PropertyRedressScheme => Json.obj("isRedress" -> true,"propertyRedressScheme" -> "03")
      case Other(value) =>
        Json.obj(
          "isRedress" -> true,
          "propertyRedressScheme" -> "04",
          "propertyRedressSchemeOther" -> value
        )
    case RedressSchemedNo => Json.obj("isRedress" -> false)
  }
}


