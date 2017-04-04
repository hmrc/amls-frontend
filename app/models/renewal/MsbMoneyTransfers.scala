package models.renewal

import jto.validation.{From, Rule}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Json
import utils.MappingUtils.Implicits._

case class MsbMoneyTransfers(transfers: Int)

object MsbMoneyTransfers {

  implicit val format = Json.format[MsbMoneyTransfers]

  implicit val formReader: Rule[UrlFormEncoded, MsbMoneyTransfers] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "transfers").read[Int].withMessage("renewal.msb.transfers.value.required").map(MsbMoneyTransfers.apply)
  }

}
