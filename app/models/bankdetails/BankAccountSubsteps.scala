/*
 * Copyright 2022 HM Revenue & Customs
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

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import models.FormTypes._
import utils.MappingUtils.Implicits._

case class BankAccountIsUk(isUk: Boolean)

case class BankAccountHasIban(hasIban: Boolean)

sealed trait Account

sealed trait NonUKAccount extends Account

case class UKAccount(accountNumber: String, sortCode: String) extends Account {

  def displaySortCode: String = {
    // scalastyle:off magic.number
    val pair1 = sortCode.substring(0, 2)
    val pair2 = sortCode.substring(2, 4)
    val pair3 = sortCode.substring(4, 6)
    // scalastyle:on magic.number
    pair1 + "-" + pair2 + "-" + pair3
  }
}

case class NonUKAccountNumber(accountNumber: String) extends NonUKAccount

case class NonUKIBANNumber(IBANNumber: String) extends NonUKAccount

object BankAccountIsUk {

  implicit val isUkFormRead: Rule[UrlFormEncoded, BankAccountIsUk] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "isUK").read[Boolean].withMessage("error.bankdetails.ukbankaccount") map BankAccountIsUk.apply
    }

  implicit val isUkFormWrites: Write[BankAccountIsUk, forms.UrlFormEncoded] = Write {
    data => Map("isUK" -> Seq(data.isUk.toString))
  }

  implicit val isUkJsonReads: Reads[BankAccountIsUk] = ( __ \ "isUK").read[Boolean] map BankAccountIsUk.apply

  implicit val isUkJsonWrites = Writes[BankAccountIsUk] { data => Json.obj( "isUK" -> data.isUk) }
}

object BankAccountHasIban {

  implicit val hasIbanFormRead: Rule[UrlFormEncoded, BankAccountHasIban] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "hasIBAN").read[Boolean]
        .withMessage("error.required.bankdetails.isiban") map BankAccountHasIban.apply
    }

  implicit val hasIbanFormWrites: Write[BankAccountHasIban, UrlFormEncoded] = Write {  data => Map("hasIBAN" -> Seq(data.hasIban.toString))  }

  implicit val hasIbanJsonReads: Reads[BankAccountHasIban] = ( __ \ "isIBAN").read[Boolean] map BankAccountHasIban.apply

  implicit val hasIbanJsonWrites = Writes[BankAccountHasIban] { data => Json.obj("isIBAN" -> data.hasIban ) }
}

object Account {

  val sortCodeRegex = "^[0-9]{6}".r
  val ukBankAccountNumberRegex = "^[0-9]{8}$".r
  val nonUKBankAccountNumberRegex = "^[0-9a-zA-Z_]+$".r
  val ibanRegex = "^[0-9a-zA-Z_]+$".r
  val sortCodeLength = 6
  val maxNonUKBankAccountNumberLength = 40
  val maxUKBankAccountNumberLength = 8
  val maxIBANLength = 34

  val sortCodeType = (removeDashRule andThen removeSpacesRule andThen notEmpty.withMessage("error.invalid.bankdetails.sortcode"))
    .andThen(minLength(sortCodeLength)).withMessage("error.invalid.bankdetails.sortcode.length")
    .andThen(maxLength(sortCodeLength)).withMessage("error.invalid.bankdetails.sortcode.length")
    .andThen(pattern(sortCodeRegex).withMessage("error.invalid.bankdetails.sortcode.characters"))

  val ukBankAccountNumberType = notEmpty.withMessage("error.bankdetails.accountnumber")
    .andThen(minLength(maxUKBankAccountNumberLength).withMessage("error.max.length.bankdetails.accountnumber"))
    .andThen(maxLength(maxUKBankAccountNumberLength).withMessage("error.max.length.bankdetails.accountnumber"))
    .andThen(pattern(ukBankAccountNumberRegex).withMessage("error.invalid.bankdetails.accountnumber"))

  val nonUKBankAccountNumberType = notEmpty.withMessage("error.bankdetails.accountnumber")
    .andThen(maxLength(maxNonUKBankAccountNumberLength).withMessage("error.invalid.bankdetails.account.length"))
    .andThen(pattern(nonUKBankAccountNumberRegex).withMessage("error.invalid.bankdetails.account"))

  val ibanType = notEmpty.withMessage("error.required.bankdetails.iban")
    .andThen(maxLength(maxIBANLength).withMessage("error.max.length.bankdetails.iban"))
    .andThen(pattern(ibanRegex).withMessage("error.invalid.bankdetails.iban"))

  implicit val ukFormRead: Rule[UrlFormEncoded, UKAccount] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (
        (__ \ "accountNumber").read(ukBankAccountNumberType) ~
        (__ \ "sortCode").read(sortCodeType)
      ) (UKAccount.apply)
  }

  implicit val ukFormWrites: Write[UKAccount, UrlFormEncoded] = Write {
    data => Map(
      "accountNumber" -> data.accountNumber,
      "sortCode" -> data.sortCode
    )
  }

  val ukJsonReads: Reads[Account] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "accountNumber").read[String] ~
        (__ \ "sortCode").read[String]
      ) (UKAccount.apply _)
  }

  val ukJsonWrites = Writes[UKAccount] {
    data => Json.obj(
      "accountNumber" -> data.accountNumber,
      "sortCode" -> data.sortCode)
  }

  implicit val nonUkAccountFormRead: Rule[UrlFormEncoded, NonUKAccountNumber] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "nonUKAccountNumber").read(nonUKBankAccountNumberType) map NonUKAccountNumber.apply
  }

  implicit val nonUkAccountFormWrites: Write[NonUKAccountNumber, UrlFormEncoded] = Write {
    data => Map("nonUKAccountNumber" -> data.accountNumber)
  }

  val nonUkAccountJsonWrites = Writes[NonUKAccountNumber] { data => Json.obj("nonUKAccountNumber" -> data.accountNumber) }

  val nonUkAccountJsonReads: Reads[Account] = (__ \ "nonUKAccountNumber").read[String] map NonUKAccountNumber.apply

  implicit val nonUkIbanRead: Rule[UrlFormEncoded, NonUKIBANNumber] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "IBANNumber").read(ibanType) map NonUKIBANNumber.apply
    }

  implicit val nonUkIbanWrites: Write[NonUKIBANNumber, UrlFormEncoded] = Write { data => Map("IBANNumber" -> data.IBANNumber) }

  val nonUkIbanJsonReads: Reads[Account] = (__ \ "IBANNumber").read[String] map NonUKIBANNumber.apply

  val nonUkIbanJsonWrites = Writes[NonUKIBANNumber] { data => Json.obj("IBANNumber" -> data.IBANNumber)  }

  implicit val accountReads: Reads[Account] = ukJsonReads orElse nonUkIbanJsonReads orElse nonUkAccountJsonReads

  implicit val accountWrites = Writes[Account] {
    case account@UKAccount(_, _) => ukJsonWrites.writes(account)
    case account@NonUKIBANNumber(_) => nonUkIbanJsonWrites.writes(account)
    case account@NonUKAccountNumber(_) => nonUkAccountJsonWrites.writes(account)
  }
}