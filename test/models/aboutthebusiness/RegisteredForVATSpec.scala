package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class RegisteredForVATSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {


    "successfully validate given an enum value" in {

      RegisteredForVAT.formRule.validate(Map("registeredForVAT" -> Seq("false"))) must
        be(Success(RegisteredForVATNo))
    }

    "successfully validate given an `Yes` value" in {

      val data = Map(
        "registeredForVAT" -> Seq("true"),
        "registeredForVATYes" -> Seq("12345678")
      )

      RegisteredForVAT.formRule.validate(data) must
        be(Success(RegisteredForVATYes("12345678")))
    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "registeredForVAT" -> Seq("true")
      )

      RegisteredForVAT.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "registeredForVATYes") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data from enum value" in {

      RegisteredForVAT.formWrites.writes(RegisteredForVATNo) must
        be(Map("registeredForVAT" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      RegisteredForVAT.formWrites.writes(RegisteredForVATYes("12345678")) must
        be(Map("registeredForVAT" -> Seq("true"), "registeredForVATYes" -> Seq("12345678")))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[RegisteredForVAT](Json.obj("registeredForVAT" -> false)) must
        be(JsSuccess(RegisteredForVATNo, JsPath \ "registeredForVAT"))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("registeredForVAT" -> true, "registeredForVATYes" ->"12345678")

      Json.fromJson[RegisteredForVAT](json) must
        be(JsSuccess(RegisteredForVATYes("12345678"), JsPath \ "registeredForVAT" \ "registeredForVATYes"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("registeredForVAT" -> true)

      Json.fromJson[RegisteredForVAT](json) must
        be(JsError((JsPath \ "registeredForVAT" \ "registeredForVATYes") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(RegisteredForVATNo) must
        be(Json.obj("registeredForVAT" -> false))

      Json.toJson(RegisteredForVATYes("12345678")) must
        be(Json.obj(
          "registeredForVAT" -> true,
          "registeredForVATYes" -> "12345678"
        ))
    }
  }


}
