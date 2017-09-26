/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.bankdetails

import jto.validation.forms.Rules._
import models.FormTypes._
import jto.validation.ValidationError
import play.api.libs.json._
import jto.validation.forms.UrlFormEncoded
import jto.validation._


sealed trait Account

object Account {

  import utils.MappingUtils.Implicits._
  import models.FormTypes._

  val sortCodeRegex = "^[0-9]{6}".r
  val ukBankAccountNumberRegex = "^[0-9]{8}$".r
  val nonUKBankAccountNumberRegex = "^[0-9a-zA-Z_]+$".r
  val ibanRegex = "^[0-9a-zA-Z_]+$".r
  val maxNonUKBankAccountNumberLength = 40
  val maxUKBankAccountNumberLength = 8
  val maxIBANLength = 34

  val sortCodeType = (removeDashRule andThen removeSpacesRule andThen notEmpty)
    .withMessage("error.invalid.bankdetails.sortcode")
    .andThen(pattern(sortCodeRegex).withMessage("error.invalid.bankdetails.sortcode"))

  val ukBankAccountNumberType = notEmpty
    .withMessage("error.bankdetails.accountnumber")
    .andThen(maxLength(maxUKBankAccountNumberLength).withMessage("error.max.length.bankdetails.accountnumber"))
    .andThen(pattern(ukBankAccountNumberRegex).withMessage("error.invalid.bankdetails.accountnumber"))

  val nonUKBankAccountNumberType = notEmpty
    .andThen(maxLength(maxNonUKBankAccountNumberLength).withMessage("error.invalid.bankdetails.account"))
    .andThen(pattern(nonUKBankAccountNumberRegex).withMessage("error.invalid.bankdetails.account"))

  val ibanType = notEmpty
    .andThen(maxLength(maxIBANLength).withMessage("error.invalid.bankdetails.iban"))
    .andThen(pattern(ibanRegex).withMessage("error.invalid.bankdetails.iban"))


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
          case true => (__ \ "IBANNumber").read[String] map NonUKIBANNumber.apply
          case false => (__ \ "nonUKAccountNumber").read[String] map NonUKAccountNumber.apply
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
  def displaySortCode: String = {
    // scalastyle:off magic.number
    val pair1 = sortCode.substring(0, 2)
    val pair2 = sortCode.substring(2, 4)
    val pair3 = sortCode.substring(4, 6)
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
  val maxAccountName = 40

  val accountNameType = notEmptyStrip
    .andThen(notEmpty.withMessage("error.bankdetails.accountname"))
    .andThen(maxLength(maxAccountName).withMessage("error.invalid.bankdetails.accountname"))
    .andThen(basicPunctuationPattern())

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
      ) (unlift(BankAccount.unapply))
  }

  implicit val jsonReads: Reads[BankAccount] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    ((__ \ "accountName").read[String] and __.read[Account]) (BankAccount.apply _)
  }

  implicit val jsonWrites: Writes[BankAccount] = {
    import play.api.libs.functional.syntax._

    (
      (__ \ "accountName").write[String] and
        __.write[Account]
      ) (unlift(BankAccount.unapply))

  }

}
