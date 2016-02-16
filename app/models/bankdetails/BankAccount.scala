package models.bankdetails

import models.FormTypes._
import play.api.data.mapping.forms._
import play.api.data.mapping.{To, Write, From, Rule}
import play.api.libs.json._


sealed trait Account


object Account {

  implicit val jsonReads: Reads[Account] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__ \ "isUK").read[Boolean] flatMap {
      case true => (
        (__ \ "accountNumber").read[String] and
          (__ \ "sortCode").read[String]
        ) (UKAccount.apply _)

      case false =>
        (__ \ "accountNumber").read[String] flatMap {
          case "" =>
            (__ \ "IBANNumber").read[String] fmap IBANNumber.apply
          case _ =>
            (__ \ "accountNumber").read[String] fmap AccountNumber.apply
        }
    }
  }

  implicit val jsonWrites: Writes[Account] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[Account] {
      case ukAccount: UKAccount =>
        (
          (__ \ "accountNumber").write[String] and
            (__ \ "sortCode").write[String]
          ) (unlift(UKAccount.unapply _)).writes(ukAccount)

      //TODO Writes for Non UK Account
      /*
            case nonukAccount: NonUKAccount =>
              (__ \ "accountNumber").write[String] flatMap {
                case "" =>
                  (__ \ "IBANNumber").write[String] fmap unlift(IBANNumber.unapply _)
                case _ =>
                  (__ \ "accountNumber").write[String] fmap unlift(AccountNumber.unapply)
              }
      */
    }

  }


  implicit val formAccountRule: Rule[UrlFormEncoded, Account] = From[UrlFormEncoded] { __ =>
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


  implicit val formAccountWrite: Write[Account, UrlFormEncoded] =
    To[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Writes._
      import play.api.libs.functional.syntax.unlift
      case ukAccount: UKAccount =>
          (__ \ "accountNumber").write[String] ~
            (__ \ "sortCode").write[String]

    }

}


case class UKAccount(
                      accountNumber: String,
                      sortCode: String
                    ) extends Account

object UKAccount {

  implicit val formats = Json.format[UKAccount]

  implicit val formRuleUKAccount: Rule[UrlFormEncoded, UKAccount] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (
      (__ \ "accountNumber").read[String] and
      (__ \ "sortCode").read[String]
      ) (UKAccount.apply _)
  }

  implicit val formWriteUKAccount: Write[UKAccount, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (__.write[String] and
      __.write[String]
      ) (unlift(UKAccount.unapply _))
  }

}


sealed trait NonUKAccount extends Account

case class AccountNumber(accountNumber: String) extends NonUKAccount

case class IBANNumber(IBANNumber: String) extends NonUKAccount


case class BankAccount(accountName: String, account: Account)

object BankAccount {

  import utils.MappingUtils.Implicits._

  val key = "bank-account"

  implicit val formRuleBankAccount: Rule[UrlFormEncoded, BankAccount] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__.read[String] and
      __.read[Account]
      ).apply(BankAccount.apply _)
  }

  implicit val formWriteBankAccount: Write[BankAccount, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (__.write[String] and
      __.write[Account]
      ) (unlift(BankAccount.unapply _))
  }

  implicit val jsonReads: Reads[BankAccount] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (
      (__).read[String] and
        (__).read[Account]
      ) (BankAccount.apply _)
  }

  implicit val jsonWrites: Writes[BankAccount] = {
    import play.api.libs.functional.syntax._

    (
      (__).write[String] and
        (__).write[Account]
      ) (unlift(BankAccount.unapply _))

  }

}
