package models.moneyservicebusiness

import models.FormTypes._
import jto.validation.forms.Rules._
import jto.validation.{Write, From, Rule}
import jto.validation.forms._
import play.api.libs.json.Json

case class TransactionsInNext12Months (txnAmount: String)

object TransactionsInNext12Months {

  import utils.MappingUtils.Implicits._

  implicit val format = Json.format[TransactionsInNext12Months]

  private val txnAmountRegex = regexWithMsg("^[0-9]{1,11}$".r, "error.invalid.msb.transactions.in.12months")
  private val txnAmountType = notEmptyStrip compose
    notEmpty.withMessage("error.required.msb.transactions.in.12months") compose txnAmountRegex

  implicit val formRule: Rule[UrlFormEncoded, TransactionsInNext12Months] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
      (__ \ "txnAmount").read(txnAmountType) fmap TransactionsInNext12Months.apply
  }

  implicit val formWrites: Write[TransactionsInNext12Months, UrlFormEncoded] = Write {x =>
    Map("txnAmount" -> Seq(x.txnAmount))
  }
}

