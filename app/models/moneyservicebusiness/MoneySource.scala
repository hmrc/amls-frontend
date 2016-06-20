package models.moneyservicebusiness


import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{Path, RuleLike, Rule, From}
import play.api.libs.json.{Reads, Format}


case class BankMoneySource(bankNames : String)

case class WholesalerMoneySource(wholesalerNames : String)

case object CustomerMoneySource
