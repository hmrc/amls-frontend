package models.moneyservicebusiness

import models.FormTypes._
import play.api.data.mapping.{Write, From, Rule}
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms._
import play.api.libs.json.Json

case class CETransactionsInNext12Months (ceTransaction: String)

object CETransactionsInNext12Months {

  import utils.MappingUtils.Implicits._

  implicit val format = Json.format[CETransactionsInNext12Months]

  private val txnAmountRegex = regexWithMsg("^[0-9]{1,11}$".r, "error.invalid.msb.transactions.in.12months")
  private val txnAmountType = notEmptyStrip compose
    notEmpty.withMessage("error.required.msb.transactions.in.12months") compose txnAmountRegex

  implicit val formRule: Rule[UrlFormEncoded, CETransactionsInNext12Months] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "ceTransaction").read(txnAmountType) fmap CETransactionsInNext12Months.apply
  }

  implicit val formWrites: Write[CETransactionsInNext12Months, UrlFormEncoded] = Write {x =>
    Map("ceTransaction" -> Seq(x.ceTransaction))
  }
}
