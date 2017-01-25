package models.declaration

import org.scalatestplus.play.PlaySpec
import jto.validation.{Failure, Path, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}


class WhoIsRegisteringSpec extends PlaySpec {

  "Form Validation" must {

    "successfully validate" when {
      "successfully validate given a valid person name" in {
        val data = Map("person" -> Seq("PersonName"))
        val result = WhoIsRegistering.formRule.validate(data)
        result mustBe Success(WhoIsRegistering("PersonName"))
      }
    }

    "fail validation" when {
      "fail validation for missing data represented by an empty Map" in {
        val result = WhoIsRegistering.formRule.validate(Map.empty)
        result mustBe Failure(Seq((Path \ "person", Seq(ValidationError("error.required.declaration.who.is.registering")))))
      }
    }

    "write correct data from true value" in {
      val result = WhoIsRegistering.formWrites.writes(WhoIsRegistering("PersonName"))
      result must be(Map("person" -> Seq("PersonName")))
    }
  }

  "JSON validation" must {

    "successfully validate given an model value" in {
      val json = Json.obj("person" -> "PersonName")
      Json.fromJson[WhoIsRegistering](json) must
        be(JsSuccess(WhoIsRegistering("PersonName"), JsPath \ "person"))
    }

    "successfully validate json read write" in {
      Json.toJson(WhoIsRegistering("PersonName")) must
        be(Json.obj("person" -> "PersonName"))
    }
  }

}
