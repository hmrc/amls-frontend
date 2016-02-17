package models.bankdetails

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.Success

class BankAccountSpec extends PlaySpec with MockitoSugar {

  "Account" must {

    "Form Rule validation successfully for UKAccount" in {
      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("11-22-33")
      )

      Account.formRule.validate(urlFormEncoded) must be(Success(UKAccount("12345678", "11-22-33")))
    }


    "Form Write validation successfully for UKAccount" in {

      val ukAccount = UKAccount("12345634535353453535354378", "11-22-33")

      val urlFormEncoded = Map(
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345634535353453535354378"),
        "sortCode" -> Seq("11-22-33")
      )

      Account.formWrites.writes(ukAccount) must be(urlFormEncoded)
    }


    "Form Rule validation for Non UKAccount" in {

      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("false"),
        "accountNumber" -> Seq("12345678")
      )

      Account.formRule.validate(urlFormEncoded) must be(Success(AccountNumber("12345678")))
    }

    "Form Write validation for Non UKAccount" in {

      val nonUKAccount = IBANNumber("3242423424290788979345897345907")

      val urlFormEncoded = Map(
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq("3242423424290788979345897345907")
      )

      Account.formWrites.writes(nonUKAccount) must be(urlFormEncoded)
    }


  }


  "BankAccount" must {

    "Form Rule validation for UKAccount" in {

      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("11-22-33")
      )

      BankAccount.formRule.validate(urlFormEncoded) must be(Success(BankAccount("test", UKAccount("12345678", "11-22-33"))))
    }



    "Form Rule validation Non UK Account" in {

      val urlFormEncoded = Map(
        "accountName" -> Seq("My Account"),
        "isUK" -> Seq("false"),
        "accountNumber" -> Seq("12345678123456781234567812345678"),
        "sortCode" -> Seq("11-22-33")
      )

      BankAccount.formRule.validate(urlFormEncoded) must be(Success(BankAccount("My Account", AccountNumber("12345678123456781234567812345678"))))
    }

  }

}
