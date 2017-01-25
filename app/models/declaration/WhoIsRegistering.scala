package models.declaration

import jto.validation._
import jto.validation.forms.Rules.{minLength => _}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Json

case class WhoIsRegistering(person : String)

object WhoIsRegistering {

  import utils.MappingUtils.Implicits._

  val key = "who-is-registering"

  implicit val formRule: Rule[UrlFormEncoded, WhoIsRegistering] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "person").read[String].withMessage("error.required.declaration.who.is.registering") fmap WhoIsRegistering.apply
    }
  implicit val formWrites: Write[WhoIsRegistering, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
        (__ \ "person").write[String] contramap{x =>x.person}
  }

  implicit val format = Json.format[WhoIsRegistering]

}
