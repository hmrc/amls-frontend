package models.bankdetails

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError

import play.api.libs.json.{JsSuccess, JsPath, Json}


class BankAccountSpec extends PlaySpec with MockitoSugar {

  "For the Account" must {

    "Form Rule validation is successful for UKAccount" in {
      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("112233")
      )

      Account.formRead.validate(urlFormEncoded) must be(Success(UKAccount("12345678", "112233")))
    }

    "fail on invalid selection" in {
      Account.formRead.validate(Map("accountName" -> Seq("test"), "isUK" -> Seq("false"), "nonUKAccountNumber" -> Seq(""))) must be(Failure(Seq(
        (Path \ "IBANNumber") -> Seq(ValidationError("error.required")))))
    }


    "Form Rule validation is successful for UKAccount1" in {
      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq(""),
        "nonUKAccountNumber" -> Seq("123456789012345678901234567890ABCDefghij")
      )

      Account.formRead.validate(urlFormEncoded) must be(Success(NonUKAccountNumber("123456789012345678901234567890ABCDefghij")))
    }

    "Form Rule validation is successful for UKAccount2" in {
      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq("123456789012345678901234567890ABCD"),
        "nonUKAccountNumber" -> Seq("")
      )

      Account.formRead.validate(urlFormEncoded) must be(Success(NonUKIBANNumber("123456789012345678901234567890ABCD")))
    }

    "Form Write is successful for UKAccount" in {

      val ukAccount = UKAccount("12345678", "112233")

      val urlFormEncoded = Map(
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("112233")
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
        "sortCode" -> "112233"
      )

      Account.jsonReads.reads(jsObject) must be(JsSuccess(UKAccount("12345678", "112233"), JsPath \ "isUK"))
    }

    "JSON Write is successful for UKAccount" in {

      val ukAccount = UKAccount("12345678", "112233")

      val jsObject = Json.obj(
        "isUK" -> true,
        "accountNumber" -> "12345678",
        "sortCode" -> "112233"
      )

      Account.jsonWrites.writes(ukAccount) must be(jsObject)
    }

    "Form Rule validation for Non UKAccount IBAN Number" in {

      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("false"),
        "nonUKAccountNumber" -> Seq(""),
        "IBANNumber" -> Seq("12334623784623648236482364872364726384762384762384623874554787876868")
      )

      Account.formRead.validate(urlFormEncoded) must be(Failure(Seq(
        (Path \ "IBANNumber") -> Seq(ValidationError("error.maxLength", 34)))))
    }

    "Form Rule validation for Non UKAccount Account Number" in {

      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq(""),
        "nonUKAccountNumber" -> Seq("12334623784623648236482364872364726384762384762384623874554787876868")
      )

      Account.formRead.validate(urlFormEncoded) must be(Failure(Seq(
        (Path \ "nonUKAccountNumber") -> Seq(ValidationError("error.maxLength", 40)))))
    }

    "Form Write validation for IBAN Non UK Account" in {

      val nonUKIBANNumber = NonUKIBANNumber("3242423424290788979345897345907")

      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq("3242423424290788979345897345907")
      )

      Account.formWrites.writes(nonUKIBANNumber) must be(urlFormEncoded)
    }

    "JSON Read is successful for Non UKAccount" in {
      val jsObject = Json.obj(
        "accountName" -> "test",
        "isUK" -> false,
        "IBANNumber" -> "IB12345678",
        "isIBAN" -> true
      )

      Account.jsonReads.reads(jsObject) must be(JsSuccess(NonUKIBANNumber("IB12345678"), JsPath \ "isUK" \ "isIBAN" \"IBANNumber"))
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
        "sortCode" -> Seq("112233")
      )

      BankAccount.formRule.validate(urlFormEncoded) must be(Success(BankAccount("test", UKAccount("12345678", "112233"))))
    }

    "Form Write validation for UKAccount" in {

      val ukAccount = UKAccount("12345678", "112233")
      val bankAccount = BankAccount("My Account", ukAccount)

      val urlFormEncoded = Map(
        "accountName" -> Seq("My Account"),
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("112233")
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

      BankAccount.formRule.validate(urlFormEncoded) must be(Success(BankAccount("My Account", NonUKAccountNumber("123456789012345678901234567890ABCDEFGHIJ"))))
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
