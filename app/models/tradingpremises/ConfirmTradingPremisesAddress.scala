package models.tradingpremises

import jto.validation.{From, Rule, Write}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Json

case class ConfirmTradingPremisesAddress(confirmAddress: Boolean)

object ConfirmTradingPremisesAddress {

  implicit val formats = Json.format[ConfirmTradingPremisesAddress]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ConfirmTradingPremisesAddress] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "confirmAddress").read[Boolean].withMessage("error.required.tp.confirm.address") map ConfirmTradingPremisesAddress.apply
    }

  implicit val formWrites: Write[ConfirmTradingPremisesAddress, UrlFormEncoded] =
    Write {
      case ConfirmTradingPremisesAddress(b) =>
        Map("confirmAddress" -> Seq(b.toString))
    }
}