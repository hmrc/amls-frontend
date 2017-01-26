package models.bankdetails

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play. PlaySpec
import jto.validation.{Path, Failure, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class BankAccountTypeSpec extends PlaySpec with MockitoSugar {

  "Bank account types form" must {
    "fail to validate" when {
      "an invalid selectiion is made for the type of bank account the business uses" in {
        val urlFormEncoded = Map(
          "bankAccountType" -> Seq("")
        )

        BankAccountType.formReads.validate(urlFormEncoded) must
          be(Failure(Seq(
            (Path \ "bankAccountType") -> Seq(ValidationError("error.invalid"))
          )))
      }

      "no selection is made for the type of bank account the business uses" in {
        val urlFormEncoded = Map(
          "bankAccountType" -> Seq()
        )

        BankAccountType.formReads.validate(urlFormEncoded) must
          be(Failure(Seq(
            (Path \ "bankAccountType") -> Seq(ValidationError("error.bankdetails.accounttype"))
          )))
      }

    }
  }

  "BankAccountType" must {

    "successfully validate form Read" in {
      BankAccountType.formReads.validate(Map("bankAccountType" -> Seq("01"))) must be (Success(Some(PersonalAccount)))
      BankAccountType.formReads.validate(Map("bankAccountType" -> Seq("02"))) must be (Success(Some(BelongsToBusiness)))
      BankAccountType.formReads.validate(Map("bankAccountType" -> Seq("03"))) must be (Success(Some(BelongsToOtherBusiness)))
      BankAccountType.formReads.validate(Map("bankAccountType" -> Seq("04"))) must be (Success(None))

    }

    "fail on invalid selection" in {
        BankAccountType.formReads.validate(Map("bankAccountType" -> Seq("05"))) must be(Failure(Seq(
            (Path \ "bankAccountType") -> Seq(ValidationError("error.invalid"))
          )))
    }

    "successfully write form data" in {
      BankAccountType.formWrites.writes(Some(PersonalAccount)) must be (Map("bankAccountType" -> Seq("01")))
      BankAccountType.formWrites.writes(Some(BelongsToBusiness)) must be (Map("bankAccountType" -> Seq("02")))
      BankAccountType.formWrites.writes(Some(BelongsToOtherBusiness)) must be (Map("bankAccountType" -> Seq("03")))
      BankAccountType.formWrites.writes(None) must be (Map.empty)
    }

    "validate Json read" in {
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" -> "01")) must
        be (JsSuccess(PersonalAccount, JsPath \ "bankAccountType"))
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" -> "02")) must
        be (JsSuccess(BelongsToBusiness, JsPath \ "bankAccountType"))
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" -> "03")) must
        be (JsSuccess(BelongsToOtherBusiness, JsPath \ "bankAccountType"))

    }

    "fail Json read on invalid data" in  {
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" ->"10")) must
        be (JsError(JsPath \ "bankAccountType", play.api.data.validation.ValidationError("error.invalid")))
    }

    "write correct Json value" in  {
      Json.toJson(PersonalAccount) must be (Json.obj("bankAccountType" -> "01"))
      Json.toJson(BelongsToBusiness) must be (Json.obj("bankAccountType" -> "02"))
      Json.toJson(BelongsToOtherBusiness) must be (Json.obj("bankAccountType" -> "03"))
    }
  }

}
