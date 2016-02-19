package models.bankdetails

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.Success
<<<<<<< HEAD
import play.api.libs.json.{JsPath, JsSuccess, Json}
=======
import play.api.libs.json.{JsSuccess, JsPath, Json}
>>>>>>> AMLS-234

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


    "Form Write is successful for UKAccount" in {

      val ukAccount = UKAccount("12345678", "11-22-33")

      val urlFormEncoded = Map(
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("11-22-33")
      )

      Account.formWrites.writes(ukAccount) must be(urlFormEncoded)
    }


    "JSON Read is successful for UKAccount" in {
      val jsObject = Json.obj(
        "isUK" -> true,
        "accountNumber" -> "12345678",
        "sortCode" -> "11-22-33"
      )

      Account.jsonReads.reads(jsObject) must be(JsSuccess(UKAccount("12345678", "11-22-33"), JsPath \ "isUK"))
    }


    "JSON Write is successful for UKAccount" in {

      val ukAccount = UKAccount("12345678", "11-22-33")

      val jsObject = Json.obj(
        "isUK" -> true,
        "accountNumber" -> "12345678",
        "sortCode" -> "11-22-33"
      )

      Account.jsonWrites.writes(ukAccount) must be(jsObject)
    }


    "Form Rule validation for Non UKAccount" in {

      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("false"),
        "nonUKAccountNumber" -> Seq("12345678")
      )

      Account.formRule.validate(urlFormEncoded) must be(Success(NonUKAccountNumber("12345678")))
    }

    "Form Rule validation for Non UKAccount IBAN Number" in {

      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("false"),
        "IBANNumber" -> Seq("1234554787876868")
      )

      Account.formRule.validate(urlFormEncoded) must be(Success(NonUKIBANNumber("1234554787876868")))
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
        "nonUKAccountNumber" -> "",
        "IBANNumber" -> "IB12345678"
      )

      Account.jsonReads.reads(jsObject) must be(JsSuccess(NonUKIBANNumber("IB12345678"), JsPath \ "isUK" \ "nonUKAccountNumber" \"IBANNumber"))
    }

    "JSON Write is successful for Non UK Account Number" in {

      val nonUKAccountNumber = NonUKAccountNumber("12345678")

      val jsObject = Json.obj(
        "isUK" -> false,
        "nonUKAccountNumber" -> "12345678"
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


    "validate Json read" in {
      Json.fromJson[BankAccount](Json.obj("accountName" -> "test", "isUK" -> false, "nonUKAccountNumber" -> "", "IBANNumber" -> "12345678")) must
        be (JsSuccess(NonUKIBANNumber("12345678")))

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
