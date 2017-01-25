package models.businessactivities

import jto.validation.{Path, Write, From, Rule}
import jto.validation.forms._
import jto.validation.ValidationError
import play.api.libs.json.Json

case class NCARegistered(ncaRegistered: Boolean)


object NCARegistered {

  implicit val formats = Json.format[NCARegistered]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, NCARegistered] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "ncaRegistered").read[Boolean].withMessage("error.required.ba.select.nca") fmap NCARegistered.apply
  }

  implicit val formWrites: Write[NCARegistered, UrlFormEncoded] = Write {
    case NCARegistered(registered) => Map("ncaRegistered" -> Seq(registered.toString))
  }
}
