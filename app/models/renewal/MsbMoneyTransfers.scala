package models.renewal

import jto.validation.forms.Rules._
import jto.validation.forms.Writes._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, To, Write}
import models.FormTypes.{notEmptyStrip, regexWithMsg}
import play.api.libs.json.Json
import utils.MappingUtils.Implicits._

case class MsbMoneyTransfers(transfers: String)

object MsbMoneyTransfers {

  implicit val format = Json.format[MsbMoneyTransfers]

  private val transferRegex = regexWithMsg("^[0-9]{1,11}$".r, "error.invalid.msb.transactions.in.12months")
  private val transferType = notEmptyStrip andThen
    notEmpty.withMessage("renewal.msb.transfers.value.invalid") andThen transferRegex

  implicit val formReader: Rule[UrlFormEncoded, MsbMoneyTransfers] = From[UrlFormEncoded] { __ =>
    (__ \ "transfers").read(transferType) map MsbMoneyTransfers.apply
  }

  implicit val formWriter: Write[MsbMoneyTransfers, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    (__ \ "transfers").write[String] contramap(_.transfers)
  }

  implicit def convert(model: MsbMoneyTransfers): models.moneyservicebusiness.TransactionsInNext12Months = {
    ???
  }

}
