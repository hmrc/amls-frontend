package models.responsiblepeople

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json

case class PersonRegistered(registerAnotherPerson: Boolean)

object PersonRegistered {

  implicit val formats = Json.format[PersonRegistered]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PersonRegistered] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "registerAnotherPerson").read[Boolean].withMessage("error.required.rp.register.another.person") map PersonRegistered.apply
    }

  implicit val formWrites: Write[PersonRegistered, UrlFormEncoded] =
    Write {
      case PersonRegistered(b) =>
        Map("registerAnotherPerson" -> Seq(b.toString))
    }
}
