package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class InvolvedInOtherSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {
    "successfully validate given an enum value" in {
      InvolvedInOther.formRule.validate(Map("involvedInOther" -> Seq("false"))) must
        be(Success(InvolvedInOtherNo))
    }

    "successfully validate given an `Yes` value" in {
      val data = Map(
        "involvedInOther" -> Seq("true"),
        "details" -> Seq("test")
      )

      InvolvedInOther.formRule.validate(data) must
        be(Success(InvolvedInOtherYes("test")))
    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "involvedInOther" -> Seq("true"),
        "details" -> Seq("")
      )

      InvolvedInOther.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "details") -> Seq(ValidationError("error.required.ba.involved.in.other.text"))
        )))
    }

    "fail to validate given an `Yes` with max value" in {

      val data = Map(
        "involvedInOther" -> Seq("true"),
        "details" -> Seq("ghgfdfdfh"*50)
      )

      InvolvedInOther.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid.maxlength.255"))
        )))
    }

    "fail to validate mandatory field" in {

      InvolvedInOther.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "involvedInOther") -> Seq(ValidationError("error.required.ba.involved.in.other"))
        )))
    }

    "write correct data from enum value" in {

      InvolvedInOther.formWrites.writes(InvolvedInOtherNo) must
        be(Map("involvedInOther" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      InvolvedInOther.formWrites.writes(InvolvedInOtherYes("test")) must
        be(Map("involvedInOther" -> Seq("true"), "details" -> Seq("test")))
    }

  }

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[InvolvedInOther](Json.obj("involvedInOther" -> false)) must
        be(JsSuccess(InvolvedInOtherNo, JsPath \ "involvedInOther"))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("involvedInOther" -> true, "details" ->"test")

      Json.fromJson[InvolvedInOther](json) must
        be(JsSuccess(InvolvedInOtherYes("test"), JsPath \ "involvedInOther" \ "details"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("involvedInOther" -> true)

      Json.fromJson[InvolvedInOther](json) must
        be(JsError((JsPath \ "involvedInOther" \ "details") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(InvolvedInOtherNo) must
        be(Json.obj("involvedInOther" -> false))

      Json.toJson(InvolvedInOtherYes("test")) must
        be(Json.obj(
          "involvedInOther" -> true,
          "details" -> "test"
        ))
    }
  }

}
