package models.bankdetails

import models.FormTypes._
import play.api.libs.json._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{To, Write, From, Rule}


sealed trait Account

object Account {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, Account] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "isUK").read[Boolean] flatMap {
      case true =>
        (
          (__ \ "accountNumber").read(ukBankAccountNumberType) and
            (__ \ "sortCode").read(sortCodeType)
          ) (UKAccount.apply _)
      case false =>
        (__ \ "nonUKAccountNumber").read[String] flatMap {
          case "" =>
            (__ \ "IBANNumber").read(ibanType) fmap NonUKIBANNumber.apply
          case _ =>
            (__ \ "nonUKAccountNumber").read(nonUKBankAccountNumberType) fmap NonUKAccountNumber.apply
        }
      }
  }

  implicit val formWrites: Write[Account, UrlFormEncoded] = Write {
    case f: UKAccount =>
      Map(
        "isUK" -> Seq("true"),
        "accountNumber" -> f.accountNumber,
        "sortCode" -> f.sortCode
      )
    case f: NonUKAccount =>
      f match {
        case nonukacc: NonUKAccountNumber =>
          Map(
            "isUK" -> Seq("false"),
            "nonUKAccountNumber" -> nonukacc.accountNumber)
        case iban: NonUKIBANNumber =>
          Map(
            "isUK" -> Seq("false"),
            "nonUKAccountNumber" -> Seq(""),
            "IBANNumber" -> iban.IBANNumber)
      }

  }

  implicit val jsonReads: Reads[Account] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__ \ "isUK").read[Boolean] flatMap {
      case true => (
        (__ \ "accountNumber").read[String] and
          (__ \ "sortCode").read[String]
        ) (UKAccount.apply _)

      case false =>
        (__ \ "nonUKAccountNumber").read[String] flatMap {
          case "" =>
            (__ \ "IBANNumber").read[String] fmap NonUKIBANNumber.apply
          case _ =>
            (__ \ "nonUKAccountNumber").read[String] fmap NonUKAccountNumber.apply
        }
    }
  }

  implicit val jsonWrites = Writes[Account] {
    case m: UKAccount =>
      Json.obj("isUK" -> true,
        "accountNumber" -> m.accountNumber,
        "sortCode" -> m.sortCode)
    case m: NonUKAccount => {
      m match {
        case acc: NonUKAccountNumber => Json.obj("isUK" -> false,
          "nonUKAccountNumber" -> acc.accountNumber)
        case iban: NonUKIBANNumber => Json.obj("isUK" -> false,
          "nonUKAccountNumber" -> "",
          "IBANNumber" -> iban.IBANNumber)
      }
    }

  }

}

case class UKAccount(
                      accountNumber: String,
                      sortCode: String
                    ) extends Account


sealed trait NonUKAccount extends Account

case class NonUKAccountNumber(accountNumber: String) extends NonUKAccount

case class NonUKIBANNumber(IBANNumber: String) extends NonUKAccount


case class BankAccount(accountName: String, account: Account)

object BankAccount {

  import utils.MappingUtils.Implicits._

  val key = "bank-account"

  implicit val formRule: Rule[UrlFormEncoded, BankAccount] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    ((__ \ "accountName").read(accountNameType) and
      __.read[Account]
      ).apply(BankAccount.apply _)
  }

  implicit val formWrite: Write[BankAccount, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    ((__ \ "accountName").write[String] and
      __.write[Account]
      ) (unlift(BankAccount.unapply _))
  }

  implicit val jsonReads: Reads[BankAccount] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (
      (__ \ "accountName").read[String] and
        (__).read[Account]
      ) (BankAccount.apply _)
  }

  implicit val jsonWrites: Writes[BankAccount] = {
    import play.api.libs.functional.syntax._

    (
      (__ \ "accountName").write[String] and
        (__).write[Account]
      ) (unlift(BankAccount.unapply _))

  }
}
