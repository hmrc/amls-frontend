package models.moneyservicebusiness


import jto.validation.forms.UrlFormEncoded
import jto.validation.{Path, RuleLike, Rule, From}
import play.api.libs.json.{Reads, Format}


case class BankMoneySource(bankNames : String)

case class WholesalerMoneySource(wholesalerNames : String)

case object CustomerMoneySource
