package models.renewal

import jto.validation.forms.Rules._
import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import models.FormTypes._
import play.api.libs.json.Json

case class CETransactions (ceTransaction: String)

object CETransactions {

  import utils.MappingUtils.Implicits._

  implicit val format = Json.format[CETransactions]

  private val txnAmountRegex = regexWithMsg("^[0-9]{1,11}$".r, "error.invalid.msb.transactions.in.12months")
  private val txnAmountType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.renewal.transactions.in.12months") andThen txnAmountRegex

  implicit val formRule: Rule[UrlFormEncoded, CETransactions] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "ceTransaction").read(txnAmountType) map CETransactions.apply
  }

  implicit val formWrites: Write[CETransactions, UrlFormEncoded] = Write {x =>
    Map("ceTransaction" -> Seq(x.ceTransaction))
  }

  implicit def convert(model: CETransactions): models.moneyservicebusiness.CETransactionsInNext12Months = {
    ???
  }
}
