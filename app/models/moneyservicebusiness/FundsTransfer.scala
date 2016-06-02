package models.moneyservicebusiness

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class FundsTransfer(transferWithoutFormalSystems: Boolean)

object FundsTransfer {
  implicit val formats = Json.format[FundsTransfer]

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, FundsTransfer] =
    From[UrlFormEncoded] { __ =>
import play.api.data.mapping.forms.Rules._
(__ \ "transferWithoutFormalSystems").read[Boolean].withMessage("error.required.msb.fundsTransfer") fmap FundsTransfer.apply
}

  implicit val formWrites: Write[FundsTransfer, UrlFormEncoded] =
    Write {
      case FundsTransfer(b) =>
        Map("transferWithoutFormalSystems" -> Seq(b.toString))
    }
}
