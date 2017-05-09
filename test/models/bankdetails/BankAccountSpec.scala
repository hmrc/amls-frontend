package models.bankdetails

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError

import play.api.libs.json.{JsSuccess, JsPath, Json}


class BankAccountSpec extends PlaySpec with MockitoSugar {

  "Account details form" must {
    "fail to validate" when {
      "account name is given an empty string" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq(""),
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq("12345678"),
          "sortCode" -> Seq("000000")
        )

        BankAccount.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "accountName") -> Seq(ValidationError("error.bankdetails.accountname"))
          )))
      }
      "account name is given a value greater than max length" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("A" * 41),
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq("12345678"),
          "sortCode" -> Seq("000000")
        )

        BankAccount.formRule.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "accountName") -> Seq(ValidationError("error.invalid.bankdetails.accountname"))
          )))
      }
      "no selection is made for an account type" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("test"),
          "isUK" -> Seq("")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "isUK") -> Seq(ValidationError("error.bankdetails.ukbankaccount")))))
      }
      "a UK account is selected and an empty account number is provided" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("test"),
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq(""),
          "sortCode" -> Seq("000000")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "accountNumber") -> Seq(ValidationError("error.bankdetails.accountnumber")))))
      }
      "a UK account is selected and the account number given is longer than the max length" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("test"),
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq("1" * 9),
          "sortCode" -> Seq("000000")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "accountNumber") -> Seq(ValidationError("error.max.length.bankdetails.accountnumber")))))
      }
      "a UK account is selected and the account number has an incorrect pattern" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("test"),
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq("A2345678"),
          "sortCode" -> Seq("000000")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "accountNumber") -> Seq(ValidationError("error.invalid.bankdetails.accountnumber")))))
      }
      "a UK account is selected and the sort code given is empty" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("test"),
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq("12345678"),
          "sortCode" -> Seq("")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "sortCode") -> Seq(ValidationError("error.invalid.bankdetails.sortcode")))))
      }
      "a UK account is selected and the value given has an incorrect pattern" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("test"),
          "isUK" -> Seq("true"),
          "accountNumber" -> Seq("12345678"),
          "sortCode" -> Seq("AABB23")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "sortCode") -> Seq(ValidationError("error.invalid.bankdetails.sortcode")))))
      }
      "a non UK account is selected and both the IBAN and non UK account number are empty" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("test"),
          "isUK" -> Seq("false"),
          "IBANNumber" -> Seq(""),
          "nonUKAccountNumber" -> Seq("")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "IBANNumber") -> Seq(ValidationError("error.required.bankdetails.iban.account")))))
      }
      "a non UK account is selected and the IBAN given is greater than the max length permitted" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("test"),
          "isUK" -> Seq("false"),
          "nonUKAccountNumber" -> Seq(""),
          "IBANNumber" -> Seq("12334623784623648236482364872364726384762384762384623874554787876868")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "IBANNumber") -> Seq(ValidationError("error.invalid.bankdetails.iban")))))
      }
      "a non UK account is selected and the IBAN given has an incorrect pattern" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("test"),
          "isUK" -> Seq("false"),
          "nonUKAccountNumber" -> Seq(""),
          "IBANNumber" -> Seq("12345678--")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "IBANNumber") -> Seq(ValidationError("error.invalid.bankdetails.iban")))))
      }
      "a non UK account is selected and the non UK account number is greater than the max length permitted" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("test"),
          "isUK" -> Seq("false"),
          "nonUKAccountNumber" -> Seq("A" * 41),
          "IBANNumber" -> Seq("")
        )

        Account.formRead.validate(urlFormEncoded) must be(Invalid(Seq(
          (Path \ "nonUKAccountNumber") -> Seq(ValidationError("error.invalid.bankdetails.account")))))
      }
      "a non UK account is selected and the non UK account number given has an incorrect pattern" in {
        val urlFormEncoded = Map(
          "accountName" -> Seq("test"),
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
        "accountName" -> Seq("test"),
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
        "accountName" -> Seq("test"),
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq(""),
        "nonUKAccountNumber" -> Seq("123456789012345678901234567890ABCDefghij")
      )

      Account.formRead.validate(urlFormEncoded) must be(Valid(NonUKAccountNumber("123456789012345678901234567890ABCDefghij")))
    }

    "Form Rule validation is successful for UKAccount2" in {
      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
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
        "accountName" -> "test",
        "isUK" -> false,
        "IBANNumber" -> "IB12345678",
        "isIBAN" -> true
      )

      Account.jsonReads.reads(jsObject) must be(JsSuccess(NonUKIBANNumber("IB12345678"), JsPath \ "IBANNumber"))
    }

    "JSON Read is successful for Non UKAccount with Account Number" in {
      val jsObject = Json.obj(
        "accountName" -> "test",
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
        "accountName" -> Seq("test"),
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("000000")
      )

      BankAccount.formRule.validate(urlFormEncoded) must be(Valid(BankAccount("test", UKAccount("12345678", "000000"))))
    }

    "Form Write validation for UKAccount" in {

      val ukAccount = UKAccount("12345678", "000000")
      val bankAccount = BankAccount("My Account", ukAccount)

      val urlFormEncoded = Map(
        "accountName" -> Seq("My Account"),
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("000000")
      )

      BankAccount.formWrite.writes(bankAccount) must be(urlFormEncoded)
    }

    "Form Rule validation Non UK Account" in {

      val urlFormEncoded = Map(
        "accountName" -> Seq("My Account"),
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq(""),
        "nonUKAccountNumber" -> Seq("123456789012345678901234567890ABCDEFGHIJ")
      )

      BankAccount.formRule.validate(urlFormEncoded) must be(Valid(BankAccount("My Account", NonUKAccountNumber("123456789012345678901234567890ABCDEFGHIJ"))))
    }

    "Form Write for Non UK Account" in {

      val nonUKBankAccount = BankAccount("My Account", NonUKAccountNumber("12341234"))

      val urlFormEncoded = Map(
        "accountName" -> Seq("My Account"),
        "isUK" -> Seq("false"),
        "nonUKAccountNumber" -> Seq("12341234")
      )

      BankAccount.formWrite.writes(nonUKBankAccount) must be(urlFormEncoded)
    }

    "Form Write validation for IBAN Non UK Account" in {

      val nonUKIBANNumber = NonUKIBANNumber("1234567812345678123456781234567812345678")
      val nonUKBankAccount = BankAccount("My Account", nonUKIBANNumber)

      val urlFormEncoded = Map(
        "accountName" -> Seq("My Account"),
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq("1234567812345678123456781234567812345678")
      )

      BankAccount.formWrite.writes(nonUKBankAccount) must be(urlFormEncoded)
    }

  }

}
