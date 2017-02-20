package models.declaration

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class BusinessNominatedOfficerSpec extends PlaySpec {

  "Form Validation" must {

    "successfully validate" when {
      "successfully validate given a valid person name" in {
        val data = Map("person" -> Seq("PersonName"))
        val result = BusinessNominatedOfficer.formRule.validate(data)
        result mustBe Valid(BusinessNominatedOfficer("PersonName"))
      }
    }

    "fail validation" when {
      "fail validation for missing data represented by an empty Map" in {
        val result = BusinessNominatedOfficer.formRule.validate(Map.empty)
        result mustBe Invalid(Seq((Path \ "person", Seq(ValidationError("error.required.declaration.nominated.officer")))))
      }
    }

    "write correct data from true value" in {
      val result = BusinessNominatedOfficer.formWrites.writes(BusinessNominatedOfficer("PersonName"))
      result must be(Map("person" -> Seq("PersonName")))
    }
  }

  "JSON validation" must {

    "successfully validate given an model value" in {
      val json = Json.obj("person" -> "PersonName")
      Json.fromJson[BusinessNominatedOfficer](json) must
        be(JsSuccess(BusinessNominatedOfficer("PersonName"), JsPath \ "person"))
    }

    "successfully validate json read write" in {
      Json.toJson(BusinessNominatedOfficer("PersonName")) must
        be(Json.obj("person" -> "PersonName"))
    }
  }

}
