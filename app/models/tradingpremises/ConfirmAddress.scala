package models.tradingpremises

import jto.validation.{From, Rule, Write}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Json

case class ConfirmAddress(confirmAddress: Boolean)

object ConfirmAddress {

  implicit val formats = Json.format[ConfirmAddress]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ConfirmAddress] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "confirmAddress").read[Boolean].withMessage("error.required.tp.confirm.address") map ConfirmAddress.apply
    }

  implicit val formWrites: Write[ConfirmAddress, UrlFormEncoded] =
    Write {
      case ConfirmAddress(b) =>
        Map("confirmAddress" -> Seq(b.toString))
    }
}