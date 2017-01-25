package models.responsiblepeople

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json

case class BankAccountRegistered(registerAnotherBank: Boolean)

object BankAccountRegistered {

  implicit val formats = Json.format[BankAccountRegistered]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, BankAccountRegistered] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "registerAnotherBank").read[Boolean].withMessage("error.required.bankdetails.register.another.bank") fmap BankAccountRegistered.apply
    }

  implicit val formWrites: Write[BankAccountRegistered, UrlFormEncoded] =
    Write {
      case BankAccountRegistered(b) =>
        Map("registerAnotherBank" -> Seq(b.toString))
    }
}

