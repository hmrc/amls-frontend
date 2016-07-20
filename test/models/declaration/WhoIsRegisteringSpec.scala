package models.declaration

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}


class WhoIsRegisteringSpec extends PlaySpec {

  "Form Validation" must {

    "successfully validate given an true value" in {
      val data = Map("person" -> Seq("divyaTadisetti"))
      val result = WhoIsRegistering.formRule.validate(data)
      result mustBe Success(WhoIsRegistering("divyaTadisetti"))
    }

    "fail validation for empty data" in {
      val result = WhoIsRegistering.formRule.validate(Map.empty)
      result mustBe Failure(Seq((Path \ "person", Seq(ValidationError("error.required.declaration.who.is.registering")))))
    }

    "write correct data from true value" in {
      val result = WhoIsRegistering.formWrites.writes(WhoIsRegistering("divyaTadisetti"))
      result must be(Map("person" -> Seq("divyaTadisetti")))
    }
  }

  "JSON validation" must {

    "successfully validate given an model value" in {
      val json = Json.obj("person" -> "divyaTadisetti")
      Json.fromJson[WhoIsRegistering](json) must
        be(JsSuccess(WhoIsRegistering("divyaTadisetti"), JsPath \ "person"))
    }

    "successfully validate json read write" in {
        Json.toJson(WhoIsRegistering("divyaTadisetti")) must
          be(Json.obj("person" -> "divyaTadisetti"))
      }
    }

}
