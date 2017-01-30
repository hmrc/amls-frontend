package models.bankdetails

import models.FormTypes._
import jto.validation.ValidationError
import play.api.libs.json._
import jto.validation.forms.UrlFormEncoded
import jto.validation._


sealed trait Account

object Account {

  import utils.MappingUtils.Implicits._

  implicit val formRead: Rule[UrlFormEncoded, Account] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "isUK").read[Boolean].withMessage("error.bankdetails.ukbankaccount") flatMap {
        case true =>
          (
            (__ \ "accountNumber").read(ukBankAccountNumberType) ~
            (__ \ "sortCode").read(sortCodeType)

            ) (UKAccount.apply _)
        case false =>
          ((__ \ "IBANNumber").read(optionR(ibanType)) ~
            (__ \ "nonUKAccountNumber").read(optionR(nonUKBankAccountNumberType))).tupled flatMap {
            case (Some(iban), _) => NonUKIBANNumber(iban)
            case (_, Some(accountNo)) => NonUKAccountNumber(accountNo)
            case (_, _) =>
              (Path \ "IBANNumber") -> Seq(ValidationError("error.required.bankdetails.iban.account"))
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
    case nonukacc: NonUKAccountNumber =>
      Map(
        "isUK" -> Seq("false"),
        "nonUKAccountNumber" -> nonukacc.accountNumber)
    case iban: NonUKIBANNumber =>
      Map(
        "isUK" -> Seq("false"),
        "IBANNumber" -> iban.IBANNumber)
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
        (__ \ "isIBAN").read[Boolean] flatMap {
          case true => (__ \ "IBANNumber").read[String] map  NonUKIBANNumber.apply
          case false =>  (__ \ "nonUKAccountNumber").read[String] map  NonUKAccountNumber.apply
        }
    }
  }

  implicit val jsonWrites = Writes[Account] {
    case m: UKAccount =>
      Json.obj(
        "isUK" -> true,
        "accountNumber" -> m.accountNumber,
        "sortCode" -> m.sortCode
      )
    case acc: NonUKAccountNumber =>
      Json.obj(
        "isUK" -> false,
        "nonUKAccountNumber" -> acc.accountNumber,
        "isIBAN" -> false
      )
    case iban: NonUKIBANNumber =>
      Json.obj(
        "isUK" -> false,
        "IBANNumber" -> iban.IBANNumber,
        "isIBAN" -> true
      )
  }
}

case class UKAccount(
                      accountNumber: String,
                      sortCode: String
                    ) extends Account {
  def displaySortCode:String = {
    // scalastyle:off magic.number
    val pair1 = sortCode.substring(0,2)
    val pair2 = sortCode.substring(2,4)
    val pair3 = sortCode.substring(4,6)
    // scalastyle:on magic.number
    pair1 + "-" + pair2 + "-" + pair3
  }
}


sealed trait NonUKAccount extends Account

case class NonUKAccountNumber(accountNumber: String) extends NonUKAccount
case class NonUKIBANNumber(IBANNumber: String) extends NonUKAccount
case class BankAccount(accountName: String, account: Account)

object BankAccount {

  import utils.MappingUtils.Implicits._

  val key = "bank-account"

  implicit val formRule: Rule[UrlFormEncoded, BankAccount] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    ((__ \ "accountName").read(accountNameType) ~
      __.read[Account]
      ).apply(BankAccount.apply _)
  }

  implicit val formWrite: Write[BankAccount, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    ((__ \ "accountName").write[String] ~
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
