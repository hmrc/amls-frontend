package models.responsiblepeople

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class PassportTypeSpec extends PlaySpec {

  "PassportType" must {

    "successfully pass validation for NoPassport" in {
      val urlFormEncoded = Map("passportType" -> Seq("03"))
      PassportType.formRule.validate(urlFormEncoded) must be(Success(NoPassport))
    }

    "successfully pass validation for uk passport number" in {
      val urlFormEncoded = Map(
        "passportType" -> Seq("01"),
        "ukPassportNumber" -> Seq("AA1234567")
      )
      PassportType.formRule.validate(urlFormEncoded) must be(Success(UKPassport("AA1234567")))
    }

    "successfully pass validation for non uk passport number" in {
      val urlFormEncoded = Map(
        "passportType" -> Seq("02"),
        "nonUKPassportNumber" -> Seq("AA1234567")
      )
      PassportType.formRule.validate(urlFormEncoded) must be(Success(NonUKPassport("AA1234567")))
    }

    "fail validation if nonUKPassportNumber is empty" in {
      val urlFormEncoded = Map(
        "passportType" -> Seq("02"),
        "nonUKPassportNumber" -> Seq("")
      )
      PassportType.formRule.validate(urlFormEncoded) must be(Failure(Seq(
        (Path \ "nonUKPassportNumber") -> Seq(ValidationError("error.required.non.uk.passport"))
      )))
    }

    "write correct no UKPassport model" in {
      val data = Map(
        "passportType" -> Seq("02"),
        "nonUKPassportNumber" -> Seq("AA1234567")
      )
      PassportType.formWrites.writes(NonUKPassport("AA1234567")) must be(data)

    }

    "write correct no NoPassport model" in {
      val data = Map(
        "passportType" -> Seq("03")
      )
      PassportType.formWrites.writes(NoPassport) must be(data)
    }

    "fail to validate given an invalid value" in {

      val urlFormEncoded = Map("passportType" -> Seq("10"))

      PassportType.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "passportType") -> Seq(ValidationError("error.invalid"))
        )))
    }

    "JSON" must {

      "Read the json and return the PassportType domain object successfully for the NoPassport" in {

        PassportType.jsonReads.reads(PassportType.jsonWrites.writes(NoPassport)) must
          be(JsSuccess(NoPassport, JsPath \ "passportType"))
      }

      "Read the json and return NonUKPassport" in {
        val model = NonUKPassport("21321313213132132")
        PassportType.jsonReads.reads(PassportType.jsonWrites.writes(model)) must
          be(JsSuccess(model, JsPath \ "passportType" \ "nonUKPassportNumber"))
      }

      "Read the json and return UKPassport" in {
        val model = UKPassport("AA2132131")
        PassportType.jsonReads.reads(PassportType.jsonWrites.writes(model)) must
          be(JsSuccess(model, JsPath \ "passportType" \ "ukPassportNumber"))
      }

      "Read the json and return error if an invalid value is found" in {
        val json = Json.obj(
          "passportType" -> "09"
        )
        PassportType.jsonReads.reads(json) must be(JsError((JsPath \ "passportType") -> ValidationError("error.invalid")))
      }
    }
  }
}
