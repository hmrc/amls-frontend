package models.renewal

import jto.validation.forms.Rules._
import jto.validation.forms.Writes._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, To, Write}
import models.FormTypes.{notEmptyStrip, regexWithMsg}
import play.api.libs.json.Json
import utils.MappingUtils.Implicits._

case class TransactionsInLast12Months(transfers: String)

object TransactionsInLast12Months {

  implicit val format = Json.format[TransactionsInLast12Months]

  private val transferRegex = regexWithMsg("^[0-9]{1,11}$".r, "error.invalid.msb.transactions.in.12months")
  private val transferType = notEmptyStrip andThen
    notEmpty.withMessage("renewal.msb.transfers.value.invalid") andThen transferRegex

  implicit val formReader: Rule[UrlFormEncoded, TransactionsInLast12Months] = From[UrlFormEncoded] { __ =>
    (__ \ "txnAmount").read(transferType) map TransactionsInLast12Months.apply
  }

  implicit val formWriter: Write[TransactionsInLast12Months, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    (__ \ "txnAmount").write[String] contramap(_.transfers)
  }

  implicit def convert(model: TransactionsInLast12Months): models.moneyservicebusiness.TransactionsInNext12Months = {
    models.moneyservicebusiness.TransactionsInNext12Months(model.transfers)
  }

}
