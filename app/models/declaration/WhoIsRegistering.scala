package models.declaration

import play.api.data.mapping._
import play.api.data.mapping.forms.Rules.{minLength => _}
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.Json

case class WhoIsRegistering(person : String)

object WhoIsRegistering {

  import utils.MappingUtils.Implicits._

  val key = "who-is-registering"

  implicit val formRule: Rule[UrlFormEncoded, WhoIsRegistering] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (__ \ "person").read[String].withMessage("error.required.declaration.who.is.registering") fmap WhoIsRegistering.apply
    }
  implicit val formWrites: Write[WhoIsRegistering, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
        (__ \ "person").write[String] contramap{x =>x.person}
  }

  implicit val format = Json.format[WhoIsRegistering]

}
