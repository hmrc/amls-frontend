package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class NationalitySpec extends PlaySpec with MockitoSugar {

  "When the user inputs the data that is posted in the form, the Nationality" must {

    "successfully pass validation for British" in {
      val urlFormEncoded = Map("nationality" -> Seq("01"))
      Nationality.formRule.validate(urlFormEncoded) must be(Success(British))
    }

    "successfully pass validation for Irish" in {
      val urlFormEncoded = Map("nationality" -> Seq("02"))
      Nationality.formRule.validate(urlFormEncoded) must be(Success(Irish))
    }

    "successfully pass validation for otherCountry" in {
      val urlFormEncoded = Map(
        "nationality" -> Seq("03"),
        "otherCountry" -> Seq("Some other value")
      )
      Nationality.formRule.validate(urlFormEncoded) must be(Success(OtherCountry("Some other value")))
    }

    "fail validation if not Other value" in {
      val urlFormEncoded = Map(
        "nationality" -> Seq("08"),
        "otherCountry" -> Seq("")
      )
      Nationality.formRule.validate(urlFormEncoded) must be(Failure(Seq(
        (Path \ "otherCountry") -> Seq(ValidationError("error.required"))
      )))
    }

    "fail to validate given an invalid value supplied that is not matching to any nationality" in {

      val urlFormEncoded = Map("nationality" -> Seq("10"))

      Nationality.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "nationality") -> Seq(ValidationError("error.invalid"))
        )))
    }
  }

  "When the user loads the page and that is posted to the form, the nationality" must {

    "load the correct value in the form for British" in {
      Nationality.formWrite.writes(British) must be(Map("nationality" -> Seq("01")))
    }

    "load the correct value in the form for Irish" in {
      Nationality.formWrite.writes(Irish) must be(Map("nationality" -> Seq("02")))
    }

    "load the correct value in the form for Other value" in {
      Nationality.formWrite.writes(OtherCountry("some value")) must be(Map(
        "nationality" -> Seq("08"),
        "otherCountry" -> Seq("some value")
      ))
    }
  }

  "JSON" must {

    "Read the json and return the RoleWithinBusiness domain object successfully for the BeneficialShareholder" in {
      val json = Json.obj(
        "nationality" -> "01"
      )
      Nationality.jsonReads.reads(json) must be(JsSuccess(British, JsPath \ "roleWithinBusiness"))
    }


    "Read the json and return the Nationality domain object successfully for the Director" in {
      val json = Json.obj(
        "nationality" -> "02"
      )
      Nationality.jsonReads.reads(json) must be(JsSuccess(Director, JsPath \ "roleWithinBusiness"))
    }

    "Read the json and return the given `other` value" in {

      val json = Json.obj(
        "nationality" -> "08",
        "otherCountry" -> "any other value"
      )

      Json.fromJson[Nationality](json) must
        be(JsSuccess(OtherCountry("GBe"), JsPath \ "roleWithinBusiness" \ "roleWithinBusinessOther"))
    }

    "Read the json and return error if an invalid value is found" in {
      val json = Json.obj(
        "roleWithinBusiness" -> "09"
      )
      Nationality.jsonReads.reads(json) must be(JsError((JsPath \ "roleWithinBusiness") -> ValidationError("error.invalid")))
    }
  }


}
