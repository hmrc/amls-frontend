package models.bankdetails

import models.FormTypes._
import play.api.data.mapping.{From, Rule}
import play.api.data.mapping.forms._

sealed trait Account

case class BankAccount(
                        accountName: AccountName,
                        account: Account
                      )

case class AccountName(
                        accountName : String
                       )

case class UKAccount(
                       accountNumber: String,
                       sortCode: String
                     ) extends Account

sealed trait NonUKAccount extends Account

case class AccountNumber(accountNumber: String) extends NonUKAccount
case class IBANNumber(IBANNumber: String) extends NonUKAccount

object BankAccount {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, BankAccount] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__.read[AccountName] and
      __.read[Account]
     ).apply(BankAccount.apply _)
  }

  implicit val accountNameRule: Rule[UrlFormEncoded, AccountName] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "accountName").read(addressType) fmap AccountName.apply
  }


  implicit val formAccountRead: Rule[UrlFormEncoded, Account] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "isUK").read[Boolean] flatMap {
      case true =>
        (
          (__ \ "accountNumber").read(addressType) and
            (__ \ "sortCode").read(addressType)
          ) (UKAccount.apply _)
      case false =>
        (__ \ "accountNumber").read[String] flatMap {
          case "" =>
            (__ \ "IBANNumber").read(addressType) fmap IBANNumber.apply
          case _ =>
            (__ \ "accountNumber").read(addressType) fmap AccountNumber.apply
        }
    }
  }
}
