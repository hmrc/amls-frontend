package models.responsiblepeople

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class BankAccountRegistered(registerAnotherBank: Boolean)

object BankAccountRegistered {

  implicit val formats = Json.format[BankAccountRegistered]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, BankAccountRegistered] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (__ \ "registerAnotherBank").read[Boolean].withMessage("error.required.bankdetails.register.another.bank") fmap BankAccountRegistered.apply
    }

  implicit val formWrites: Write[BankAccountRegistered, UrlFormEncoded] =
    Write {
      case BankAccountRegistered(b) =>
        Map("registerAnotherBank" -> Seq(b.toString))
    }
}

