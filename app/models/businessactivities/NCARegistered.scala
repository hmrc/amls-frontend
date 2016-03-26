package models.businessactivities

import play.api.data.mapping.{Path, Write, From, Rule}
import play.api.data.mapping.forms._
import play.api.data.validation.ValidationError
import play.api.libs.json.Json

case class NCARegistered(ncaRegistered: Boolean)


object NCARegistered {

  implicit val formats = Json.format[NCARegistered]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, NCARegistered] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "ncaRegistered").read[Option[Boolean]] flatMap {
      case Some(x) => NCARegistered(x)
      case _ => Path \ "ncaRegistered" -> Seq(ValidationError("error.required.ba.option.nca"))
    }
  }

  implicit val formWrites: Write[NCARegistered, UrlFormEncoded] = Write {
    case NCARegistered(registered) => Map("ncaRegistered" -> Seq(registered.toString))
  }

}