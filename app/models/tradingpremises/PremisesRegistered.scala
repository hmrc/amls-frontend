package models.responsiblepeople

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class PremisesRegistered(registerAnotherPremises: Boolean)

object PremisesRegistered {

  implicit val formats = Json.format[PremisesRegistered]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PremisesRegistered] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (__ \ "registerAnotherPremises").read[Boolean].withMessage("error.required.tp.register.another.premises") fmap PremisesRegistered.apply
    }

  implicit val formWrites: Write[PremisesRegistered, UrlFormEncoded] =
    Write {
      case PremisesRegistered(b) =>
        Map("registerAnotherPremises" -> Seq(b.toString))
    }
}
