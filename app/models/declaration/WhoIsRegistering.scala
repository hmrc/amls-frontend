package models.declaration

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules.{minLength => _}
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.Json

case class WhoIsRegistering(people : String)

object WhoIsRegistering {

  import utils.MappingUtils.Implicits._

  val key = "who-is-registering"

  implicit val formRule: Rule[UrlFormEncoded, WhoIsRegistering] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (__ \ "people").read[String].withMessage("error.required.declaration.who.is.registering") fmap WhoIsRegistering.apply
    }
  implicit val formWrites: Write[WhoIsRegistering, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
        (__ \ "people").write[String] contramap{x =>x.people}
  }

  implicit val format = Json.format[WhoIsRegistering]

}
