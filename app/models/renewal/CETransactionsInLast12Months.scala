package models.renewal

import jto.validation.forms.Rules._
import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import models.FormTypes._
import play.api.libs.json.Json

case class CETransactionsInLast12Months(ceTransaction: String)

object CETransactionsInLast12Months {

  import utils.MappingUtils.Implicits._

  implicit val format = Json.format[CETransactionsInLast12Months]

  private val txnAmountRegex = regexWithMsg("^[0-9]{1,11}$".r, "error.invalid.msb.transactions.in.12months")
  private val txnAmountType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.renewal.ce.transactions.in.12months") andThen txnAmountRegex

  implicit val formRule: Rule[UrlFormEncoded, CETransactionsInLast12Months] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "ceTransaction").read(txnAmountType) map CETransactionsInLast12Months.apply
  }

  implicit val formWrites: Write[CETransactionsInLast12Months, UrlFormEncoded] = Write { x =>
    Map("ceTransaction" -> Seq(x.ceTransaction))
  }

  implicit def convert(model: CETransactionsInLast12Months): models.moneyservicebusiness.CETransactionsInNext12Months = {
    models.moneyservicebusiness.CETransactionsInNext12Months(model.ceTransaction)
  }
}
