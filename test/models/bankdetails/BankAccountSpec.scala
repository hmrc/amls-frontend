package models.bankdetails

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.Success

class BankAccountSpec extends PlaySpec with MockitoSugar {

  "For the Account" must {

    "Form Rule validation is successful for UKAccount" in {
      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("11-22-33")
      )

      Account.formRule.validate(urlFormEncoded) must be(Success(UKAccount("12345678", "11-22-33")))
    }


    "Form Write validation is successful for UKAccount" in {

      val ukAccount = UKAccount("12345678", "11-22-33")

      val urlFormEncoded = Map(
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("11-22-33")
      )

      Account.formWrites.writes(ukAccount) must be(urlFormEncoded)
    }


    "Form Rule validation for Non UKAccount" in {

      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("false"),
        "nonUKAccountNumber" -> Seq("12345678")
      )

      Account.formRule.validate(urlFormEncoded) must be(Success(NonUKAccountNumber("12345678")))
    }

    "Form Write validation for IBAN Non UK Account" in {

      val nonUKIBANNumber = NonUKIBANNumber("3242423424290788979345897345907")

      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq("3242423424290788979345897345907")
      )

      Account.formWrites.writes(nonUKIBANNumber) must be(urlFormEncoded)
    }


    "Form Write validation for Non UK Account Number" in {

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
        "sortCode" -> Seq("11-22-33")
      )

      BankAccount.formRule.validate(urlFormEncoded) must be(Success(BankAccount("test", UKAccount("12345678", "11-22-33"))))
    }

    "Form Write validation for UKAccount" in {

      val ukAccount = UKAccount("12345678", "11-22-33")
      val bankAccount = BankAccount("My Account", ukAccount)

      val urlFormEncoded = Map(
        "accountName" -> Seq("My Account"),
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("11-22-33")
      )

      BankAccount.formWrite.writes(bankAccount) must be(urlFormEncoded)
    }


    "Form Rule validation Non UK Account" in {

      val urlFormEncoded = Map(
        "accountName" -> Seq("My Account"),
        "isUK" -> Seq("false"),
        "nonUKAccountNumber" -> Seq("12345678123456781234567812345678"),
        "sortCode" -> Seq("11-22-33")
      )

      BankAccount.formRule.validate(urlFormEncoded) must be(Success(BankAccount("My Account", NonUKAccountNumber("12345678123456781234567812345678"))))
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
