package models.businessactivities

import play.api.data.mapping.{Write, From, Rule}
import play.api.data.mapping.forms._
import play.api.libs.json.Json

case class NCARegistered(ncaRegistered: Boolean)


object NCARegistered {

  implicit val formats = Json.format[NCARegistered]

  implicit val formRule: Rule[UrlFormEncoded, NCARegistered] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "ncaRegistered").read[Boolean] fmap NCARegistered.apply
  }

  implicit val formWrites: Write[NCARegistered, UrlFormEncoded] = Write {
    case NCARegistered(registered) => Map("ncaRegistered" -> Seq(registered.toString))
  }

}