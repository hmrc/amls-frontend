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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError

import play.api.libs.json.{JsSuccess, JsPath, Json}

class BankAccountSpec extends PlaySpec with MockitoSugar {

  "Account details form" must {
    "fail to validate" when {
      "no selection is made for an account type" in {
        val urlFormEncoded = Map(
          "isUK" -> Seq("")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "isUK") -> Seq(ValidationError("error.bankdetails.ukbankaccount")))))
      }
      "a UK account is selected and an empty account number is provided" in {
        val urlFormEncoded = Map(
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq(""),
          "sortCode" -> Seq("000000")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "accountNumber") -> Seq(ValidationError("error.bankdetails.accountnumber")))))
      }
      "a UK account is selected and the account number given is longer than the max length" in {
        val urlFormEncoded = Map(
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq("1" * 9),
          "sortCode" -> Seq("000000")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "accountNumber") -> Seq(ValidationError("error.max.length.bankdetails.accountnumber")))))
      }
      "a UK account is selected and the account number has an incorrect pattern" in {
        val urlFormEncoded = Map(
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq("A2345678"),
          "sortCode" -> Seq("000000")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "accountNumber") -> Seq(ValidationError("error.invalid.bankdetails.accountnumber")))))
      }
      "a UK account is selected and the sort code given is empty" in {
        val urlFormEncoded = Map(
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq("12345678"),
          "sortCode" -> Seq("")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "sortCode") -> Seq(ValidationError("error.invalid.bankdetails.sortcode")))))
      }
      "a UK account is selected and the value given has an incorrect pattern" in {
        val urlFormEncoded = Map(
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq("12345678"),
          "sortCode" -> Seq("AABB23")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "sortCode") -> Seq(ValidationError("error.invalid.bankdetails.sortcode")))))
      }
      "a non UK account is selected and both the IBAN and non UK account number are empty" in {
        val urlFormEncoded = Map(
          "isUK" -> Seq("false"),
          "IBANNumber" -> Seq(""),
          "nonUKAccountNumber" -> Seq("")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "IBANNumber") -> Seq(ValidationError("error.required.bankdetails.iban.account")))))
      }
      "a non UK account is selected and the IBAN given is greater than the max length permitted" in {
        val urlFormEncoded = Map(
          "isUK" -> Seq("false"),
          "nonUKAccountNumber" -> Seq(""),
          "IBANNumber" -> Seq("12334623784623648236482364872364726384762384762384623874554787876868")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "IBANNumber") -> Seq(ValidationError("error.invalid.bankdetails.iban")))))
      }
      "a non UK account is selected and the IBAN given has an incorrect pattern" in {
        val urlFormEncoded = Map(
          "isUK" -> Seq("false"),
          "nonUKAccountNumber" -> Seq(""),
          "IBANNumber" -> Seq("12345678--")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "IBANNumber") -> Seq(ValidationError("error.invalid.bankdetails.iban")))))
      }
      "a non UK account is selected and the non UK account number is greater than the max length permitted" in {
        val urlFormEncoded = Map(
          "isUK" -> Seq("false"),
          "nonUKAccountNumber" -> Seq("A" * 41),
          "IBANNumber" -> Seq("")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "nonUKAccountNumber") -> Seq(ValidationError("error.invalid.bankdetails.account")))))
      }
      "a non UK account is selected and the non UK account number given has an incorrect pattern" in {
        val urlFormEncoded = Map(
          "isUK" -> Seq("false"),
          "nonUKAccountNumber" -> Seq("12345678--"),
          "IBANNumber" -> Seq("")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "nonUKAccountNumber") -> Seq(ValidationError("error.invalid.bankdetails.account")))))
      }

    }
  }

  "For the Account" must {

    "Form Rule validation is successful for UKAccount" in {
      val urlFormEncoded = Map(
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("000000")
      )

      Account.formRead.validate(urlFormEncoded) must be(Valid(UKAccount("12345678", "000000")))
    }

    "displaySortCode" must {
      "return the sort code formatted for display" in {

        val account = UKAccount("12341234", "000000")

        account.displaySortCode must be("00-00-00")
      }
    }

    "Form Rule validation is successful for UKAccount1" in {
      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq(""),
        "nonUKAccountNumber" -> Seq("123456789012345678901234567890ABCDefghij")
      )

      Account.formRead.validate(urlFormEncoded) must be(Valid(NonUKAccountNumber("123456789012345678901234567890ABCDefghij")))
    }

    "Form Rule validation is successful for UKAccount2" in {
      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq("123456789012345678901234567890ABCD"),
        "nonUKAccountNumber" -> Seq("")
      )

      Account.formRead.validate(urlFormEncoded) must be(Valid(NonUKIBANNumber("123456789012345678901234567890ABCD")))
    }

    "Form Write is successful for UKAccount" in {

      val ukAccount = UKAccount("12345678", "000000")

      val urlFormEncoded = Map(
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("000000")
      )

      Account.formWrites.writes(ukAccount) must be(urlFormEncoded)
    }

    "Form Write is successful for NonUKAccount" in {

      val ukAccount = NonUKAccountNumber("12345678")

      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "nonUKAccountNumber" -> Seq("12345678")
      )

      Account.formWrites.writes(ukAccount) must be(urlFormEncoded)
    }

    "Form Write is successful for NonUKAccount iban" in {

      val ukAccount = NonUKIBANNumber("12345678")

      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq("12345678")
      )

      Account.formWrites.writes(ukAccount) must be(urlFormEncoded)
    }

    "JSON Read is successful for UKAccount" in {
      val jsObject = Json.obj(
        "isUK" -> true,
        "accountNumber" -> "12345678",
        "sortCode" -> "000000"
      )

      Account.jsonReads.reads(jsObject) must be(JsSuccess(UKAccount("12345678", "000000"), JsPath))
    }

    "JSON Write is successful for UKAccount" in {

      val ukAccount = UKAccount("12345678", "000000")

      val jsObject = Json.obj(
        "isUK" -> true,
        "accountNumber" -> "12345678",
        "sortCode" -> "000000"
      )

      Account.jsonWrites.writes(ukAccount) must be(jsObject)
    }

    "Form Write validation for IBAN Non UK Account" in {

      val nonUKIBANNumber = NonUKIBANNumber("3242423424290788979345897345907")

      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq("3242423424290788979345897345907")
      )

      Account.formWrites.writes(nonUKIBANNumber) must be(urlFormEncoded)
    }

    "JSON Read is successful for Non UKAccount with IBAN" in {
      val jsObject = Json.obj(
        "isUK" -> false,
        "IBANNumber" -> "IB12345678",
        "isIBAN" -> true
      )

      Account.jsonReads.reads(jsObject) must be(JsSuccess(NonUKIBANNumber("IB12345678"), JsPath \ "IBANNumber"))
    }

    "JSON Read is successful for Non UKAccount with Account Number" in {
      val jsObject = Json.obj(
        "isUK" -> false,
        "nonUKAccountNumber" -> "12345",
        "isIBAN" -> false
      )

      Account.jsonReads.reads(jsObject) must be(JsSuccess(NonUKAccountNumber("12345"), JsPath \ "nonUKAccountNumber"))
    }

    "JSON Write is successful for Non UK Account Number" in {

      val nonUKAccountNumber = NonUKAccountNumber("12345678")

      val jsObject = Json.obj(
        "isUK" -> false,
        "nonUKAccountNumber" -> "12345678",
        "isIBAN" -> false

      )

      Account.jsonWrites.writes(nonUKAccountNumber) must be(jsObject)
    }

    "JSON Write is successful for Non UK IBAN Number" in {

      val nonUKIBANNumber = NonUKIBANNumber("12345678")

      val jsObject = Json.obj(
        "isUK" -> false,
        "IBANNumber" -> "12345678",
        "isIBAN" -> true
      )

      Account.jsonWrites.writes(nonUKIBANNumber) must be(jsObject)
    }

    "Form Write for Non UK Account Number" in {

      val nonUKAccount = NonUKAccountNumber("3242423424290788979345897345907")

      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "nonUKAccountNumber" -> Seq("3242423424290788979345897345907")
      )
      Account.formWrites.writes(nonUKAccount) must be(urlFormEncoded)
    }
  }

  "For the BankAccount" must {

    "Form Rule validation for UKAccount" in {

      val urlFormEncoded = Map(
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("000000")
      )

      Account.formRead.validate(urlFormEncoded) must be(Valid(UKAccount("12345678", "000000")))
    }

    "Form Write validation for UKAccount" in {

      val ukAccount = UKAccount("12345678", "000000")

      val urlFormEncoded = Map(
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("000000")
      )

      Account.formWrites.writes(ukAccount) must be(urlFormEncoded)
    }

    "Form Rule validation Non UK Account" in {

      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq(""),
        "nonUKAccountNumber" -> Seq("123456789012345678901234567890ABCDEFGHIJ")
      )

      Account.formRead.validate(urlFormEncoded) must be(Valid(NonUKAccountNumber("123456789012345678901234567890ABCDEFGHIJ")))
    }

    "Form Write for Non UK Account" in {

      val nonUKBankAccount = NonUKAccountNumber("12341234")

      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "nonUKAccountNumber" -> Seq("12341234")
      )

      Account.formWrites.writes(nonUKBankAccount) must be(urlFormEncoded)
    }

    "Form Write validation for IBAN Non UK Account" in {

      val nonUKIBANNumber = NonUKIBANNumber("1234567812345678123456781234567812345678")
      val nonUKBankAccount = nonUKIBANNumber

      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq("1234567812345678123456781234567812345678")
      )

      Account.formWrites.writes(nonUKBankAccount) must be(urlFormEncoded)
    }

  }

  "ibanType" must {
    "validate IBAN supplied " in {
      Account.ibanType.validate("IBAN_0000000000000") must be(Valid("IBAN_0000000000000"))
    }

    "fail validation if IBAN is longer than the permissible length" in {
      Account.ibanType.validate("12345678901234567890123456789012345678901234567890") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.iban")))))
    }

    "fail validation if IBAN contains invalid characters" in {
      Account.ibanType.validate("ab{}kfg  ") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.iban")))))
    }

    "fail validation if IBAN contains only whitespace" in {
      Account.ibanType.validate("    ") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.iban")))))
    }
  }

  "nonUKBankAccountNumberType" must {
    "validate Non UK Account supplied " in {
      Account.nonUKBankAccountNumberType.validate("IND00000000000000") must be(Valid("IND00000000000000"))
    }

    "fail validation if Non UK Account is longer than the permissible length" in {
      Account.nonUKBankAccountNumberType.validate("12345678901234567890123456789012345678901234567890") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.account")))))
    }

    "fail validation if Non UK Account no contains invalid characters" in {
      Account.nonUKBankAccountNumberType.validate("ab{}kfg  ") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.account")))))
    }

    "fail validation if Non UK Account no contains only whitespace" in {
      Account.nonUKBankAccountNumberType.validate("    ") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.account")))))
    }
  }

  "ukBankAccountNumberType" must {

    "validate when 8 digits are supplied " in {
      Account.ukBankAccountNumberType.validate("00000000") must be(Valid("00000000"))
    }

    "fail validation when less than 8 characters are supplied" in {
      Account.ukBankAccountNumberType.validate("123456") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.accountnumber")))))
    }

    "fail validation when more than 8 characters are supplied" in {
      Account.ukBankAccountNumberType.validate("1234567890") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.max.length.bankdetails.accountnumber")))))
    }
  }

  "sortCodeType" must {

    "validate when 6 digits are supplied without - " in {
      Account.sortCodeType.validate("000000") must be(Valid("000000"))
    }

    "fail validation when more than 6 digits are supplied without - " in {
      Account.sortCodeType.validate("87654321") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.sortcode")))))
    }

    "fail when 8 non digits are supplied with - " in {
      Account.sortCodeType.validate("ab-cd-ef") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.sortcode")))))
    }

    "pass validation when dashes are used to seperate number groups" in {
      Account.sortCodeType.validate("65-43-21") must be(Valid("654321"))
    }
    "pass validation when spaces are used to seperate number groups" in {
      Account.sortCodeType.validate("65 43 21") must be(Valid("654321"))
    }

    "fail validation for sort code with any other pattern" in {
      Account.sortCodeType.validate("8712341241431243124124654321") must be(
        Invalid(Seq(Path -> Seq(ValidationError("error.invalid.bankdetails.sortcode"))))
      )
    }
  }

}
