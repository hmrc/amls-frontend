package models.bankdetails

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerTest, PlaySpec}
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class BankAccountTypeSpec extends PlaySpec with OneServerPerTest with MockitoSugar {

  "BankAccountType" must {

    "successfully validate form Read" in {
      BankAccountType.formRead.validate(Map("bankAccountType" -> Seq("01"))) must be (Success(PersonalAccount))
      BankAccountType.formRead.validate(Map("bankAccountType" -> Seq("02"))) must be (Success(BelongsToBusiness))
      BankAccountType.formRead.validate(Map("bankAccountType" -> Seq("03"))) must be (Success(BelongsToOtherBusiness))
      BankAccountType.formRead.validate(Map("bankAccountType" -> Seq("04"))) must be (Success(NoBankAccount))

    }

    "fail on invalid selection" in {
        BankAccountType.formRead.validate(Map("bankAccountType" -> Seq("05"))) must be(Failure(Seq(
            (Path \ "bankAccountType") -> Seq(ValidationError("error.invalid"))
          )))
    }

    "successfully write form data" in {
      BankAccountType.formWrite.writes(PersonalAccount) must be (Map("bankAccountType" -> Seq("01")))
      BankAccountType.formWrite.writes(BelongsToBusiness) must be (Map("bankAccountType" -> Seq("02")))
      BankAccountType.formWrite.writes(BelongsToOtherBusiness) must be (Map("bankAccountType" -> Seq("03")))
      BankAccountType.formWrite.writes(NoBankAccount) must be (Map("bankAccountType" -> Seq("04")))
    }

    "validate Json read" in {
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" -> "01")) must
        be (JsSuccess(PersonalAccount, JsPath \ "bankAccountType"))
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" -> "02")) must
        be (JsSuccess(BelongsToBusiness, JsPath \ "bankAccountType"))
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" -> "03")) must
        be (JsSuccess(BelongsToOtherBusiness, JsPath \ "bankAccountType"))
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" -> "04")) must
        be (JsSuccess(NoBankAccount, JsPath \ "bankAccountType"))

    }

    "fail Json read on invalid data" in  {
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" ->Seq("10"))) must
        be (JsError(JsPath \ "bankAccountType", ValidationError("error.invalid")))
    }

    "write correct Json value" in  {
      Json.toJson(PersonalAccount) must be (Json.obj("bankAccountType" -> "01"))
      Json.toJson(BelongsToBusiness) must be (Json.obj("bankAccountType" -> "02"))
      Json.toJson(BelongsToOtherBusiness) must be (Json.obj("bankAccountType" -> "03"))
      Json.toJson(NoBankAccount) must be (Json.obj("bankAccountType" -> "04"))
    }
  }

}
