package models.responsiblepeople

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class PersonRegistered(registerAnother: Boolean)

object PersonRegistered {

  implicit val formats = Json.format[PersonRegistered]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PersonRegistered] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (__ \ "registerAnother").read[Boolean].withMessage("error.required.rp.register.another.person") fmap PersonRegistered.apply
    }

  implicit val formWrites: Write[PersonRegistered, UrlFormEncoded] =
    Write {
      case PersonRegistered(b) =>
        Map("registerAnother" -> Seq(b.toString))
    }
}
