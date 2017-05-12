package models.responsiblepeople

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class PassportTypeSpec extends PlaySpec {

  "Uk passport number" must {
    "pass validation" when {
      "given the correct number of numbers" in {
        PassportType.ukPassportType.validate("123456789") mustBe Valid("123456789")
      }
    }

    "fail validation" when {
      "the passport number has too many characters" in {
        PassportType.ukPassportType.validate("a" * 10) mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.uk.passport")))
        )
      }
      "the passport number has too few characters" in {
        PassportType.ukPassportType.validate("a" * 8) mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.uk.passport")))
        )
      }
      "the passport number includes invalid characters (letters, punctuation etc)" in {
        PassportType.ukPassportType.validate("123abc7{}") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.invalid.uk.passport")))
        )
      }
      "the passport number is an empty string" in {
        PassportType.ukPassportType.validate("") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.uk.passport")))
        )
      }
      "the passport number is given a sequence of whitespace" in {
        PassportType.ukPassportType.validate("    ") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.uk.passport")))
        )
      }
    }
  }
  "NonUk passport number" must {
    "pass validation" when {
      "given the correct number of numbers" in {
        PassportType.noUKPassportType.validate("ab3456789") mustBe Valid("ab3456789")
      }
    }

    "fail validation" when {
      "the passport number has too many characters" in {
        PassportType.noUKPassportType.validate("a" * 50) mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.invalid.non.uk.passport")))
        )
      }
      "the passport number includes invalid characters (letters, punctuation etc)" in {
        PassportType.noUKPassportType.validate("123abc7{}") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.invalid.non.uk.passport")))
        )
      }
      "the passport number is an empty string" in {
        PassportType.noUKPassportType.validate("") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.non.uk.passport")))
        )
      }
      "the passport number is given a sequence of whitespace" in {
        PassportType.noUKPassportType.validate("    ") mustBe Invalid(
          Seq(Path -> Seq(ValidationError("error.required.non.uk.passport")))
        )
      }
    }
  }

  "PassportType" must {

    "successfully pass validation for NoPassport" in {
      val urlFormEncoded = Map("passportType" -> Seq("03"))
      PassportType.formRule.validate(urlFormEncoded) must be(Valid(NoPassport))
    }

    "successfully pass validation for uk passport number" in {
      val urlFormEncoded = Map(
        "passportType" -> Seq("01"),
        "ukPassportNumber" -> Seq("000000000")
      )
      PassportType.formRule.validate(urlFormEncoded) must be(Valid(UKPassport("000000000")))
    }

    "successfully pass validation for non uk passport number" in {
      val urlFormEncoded = Map(
        "passportType" -> Seq("02"),
        "nonUKPassportNumber" -> Seq("AA1234567")
      )
      PassportType.formRule.validate(urlFormEncoded) must be(Valid(NonUKPassport("AA1234567")))
    }

    "fail validation if nonUKPassportNumber is empty" in {
      val urlFormEncoded = Map(
        "passportType" -> Seq("02"),
        "nonUKPassportNumber" -> Seq("")
      )
      PassportType.formRule.validate(urlFormEncoded) must be(Invalid(Seq(
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
        be(Invalid(Seq(
          (Path \ "passportType") -> Seq(ValidationError("error.invalid"))
        )))
    }

    "JSON" must {

      "Read the json and return the PassportType domain object successfully for the NoPassport" in {

        PassportType.jsonReads.reads(PassportType.jsonWrites.writes(NoPassport)) must
          be(JsSuccess(NoPassport, JsPath))
      }

      "Read the json and return NonUKPassport" in {
        val model = NonUKPassport("21321313213132132")
        PassportType.jsonReads.reads(PassportType.jsonWrites.writes(model)) must
          be(JsSuccess(model, JsPath \ "nonUKPassportNumber"))
      }

      "Read the json and return UKPassport" in {
        val model = UKPassport("AA0000000")
        PassportType.jsonReads.reads(PassportType.jsonWrites.writes(model)) must
          be(JsSuccess(model, JsPath \ "ukPassportNumber"))
      }

      "Read the json and return error if an invalid value is found" in {
        val json = Json.obj(
          "passportType" -> "09"
        )
        PassportType.jsonReads.reads(json) must be(JsError((JsPath) -> play.api.data.validation.ValidationError("error.invalid")))
      }
    }
  }
}
