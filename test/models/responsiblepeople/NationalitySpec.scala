package models.responsiblepeople

import models.Country
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class NationalitySpec extends PlaySpec with MockitoSugar {

  "Form:" must {

    "successfully pass validation for British" in {
      val urlFormEncoded = Map("nationality" -> Seq("01"))
      Nationality.formRule.validate(urlFormEncoded) must be(Valid(British))
    }

    "successfully pass validation for Irish" in {
      val urlFormEncoded = Map("nationality" -> Seq("02"))
      Nationality.formRule.validate(urlFormEncoded) must be(Valid(Irish))
    }

    "successfully pass validation for otherCountry" in {
      val urlFormEncoded = Map(
        "nationality" -> Seq("03"),
        "otherCountry" -> Seq("GB")
      )
      Nationality.formRule.validate(urlFormEncoded) must be(Valid(OtherCountry(Country("United Kingdom", "GB"))))
    }

    "fail validation if not Other value" in {
      val urlFormEncoded = Map(
        "nationality" -> Seq("03"),
        "otherCountry" -> Seq("")
      )
      Nationality.formRule.validate(urlFormEncoded) must be(Invalid(Seq(
        (Path \ "otherCountry") -> Seq(ValidationError("error.required.country"))
      )))
    }

    "fail validation if invalid Other value" in {
      val urlFormEncoded = Map(
        "nationality" -> Seq("03"),
        "otherCountry" -> Seq("invalid")
      )
      Nationality.formRule.validate(urlFormEncoded) must be(Invalid(Seq(
        (Path \ "otherCountry") -> Seq(ValidationError("error.invalid.country"))
      )))
    }

    "fail validation when user has not selected at least one of the options" in {
      Nationality.formRule.validate(Map.empty) must be(Invalid(Seq(
        (Path \ "nationality") -> Seq(ValidationError("error.required.nationality"))
      )))
    }

    "fail to validate given an invalid value supplied that is not matching to any nationality" in {

      val urlFormEncoded = Map("nationality" -> Seq("10"))

      Nationality.formRule.validate(urlFormEncoded) must
        be(Invalid(Seq(
          (Path \ "nationality") -> Seq(ValidationError("error.invalid"))
        )))
    }

    "When the user loads the page and that is posted to the form, the nationality" must {

      "load the correct value in the form for British" in {
        Nationality.formWrite.writes(British) must be(Map("nationality" -> Seq("01")))
      }

      "load the correct value in the form for Irish" in {
        Nationality.formWrite.writes(Irish) must be(Map("nationality" -> Seq("02")))
      }

      "load the correct value in the form for Other value" in {
        Nationality.formWrite.writes(OtherCountry(Country("United Kingdom", "GB"))) must be(Map(
          "nationality" -> Seq("03"),
          "otherCountry" -> Seq("GB")
        ))
      }
    }
  }

  "JSON" must {

    "Read json and write the option British successfully" in {

      Nationality.jsonReads.reads(Nationality.jsonWrites.writes(British)) must be(JsSuccess(British, JsPath))
    }

    "Read json and write the option Irish successfully" in {

      Nationality.jsonReads.reads(Nationality.jsonWrites.writes(Irish)) must be(JsSuccess(Irish, JsPath))
    }

    "Read read and write the option `other country` successfully" in {
      val json = Nationality.jsonWrites.writes(OtherCountry(Country("United Kingdom", "GB")))
      Nationality.jsonReads.reads(json) must be(
        JsSuccess(OtherCountry(Country("United Kingdom", "GB")), JsPath \ "otherCountry"))
    }

    "fail to validate given an invalid value supplied that is not matching to any nationality" in {

      Nationality.jsonReads.reads(Json.obj("nationality" -> "10")) must be(JsError(List((JsPath, List(play.api.data.validation.ValidationError("error.invalid"))))))

    }
  }
}
