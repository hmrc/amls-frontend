package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class PreviouslyRegisteredSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      PreviouslyRegistered.formRule.validate(Map("previouslyRegistered" -> Seq("false"))) must
        be(Success(PreviouslyRegisteredNo))
    }

    "successfully validate given an `Yes` value" in {

      val data = Map(
        "previouslyRegistered" -> Seq("true"),
        "prevMLRRegNo" -> Seq("12345678")
      )

      PreviouslyRegistered.formRule.validate(data) must
        be(Success(PreviouslyRegisteredYes("12345678")))
    }

    "fail when mandatory fields are missing" in {

      PreviouslyRegistered.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "previouslyRegistered") -> Seq(ValidationError("error.required.atb.previously.registered"))
        )))
    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "previouslyRegistered" -> Seq("true")
      )

      PreviouslyRegistered.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data from enum value" in {

      PreviouslyRegistered.formWrites.writes(PreviouslyRegisteredNo) must
        be(Map("previouslyRegistered" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      PreviouslyRegistered.formWrites.writes(PreviouslyRegisteredYes("12345678")) must
        be(Map("previouslyRegistered" -> Seq("true"), "prevMLRRegNo" -> Seq("12345678")))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[PreviouslyRegistered](Json.obj("previouslyRegistered" -> false)) must
        be(JsSuccess(PreviouslyRegisteredNo, JsPath \ "previouslyRegistered"))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("previouslyRegistered" -> true, "prevMLRRegNo" ->"12345678")

      Json.fromJson[PreviouslyRegistered](json) must
        be(JsSuccess(PreviouslyRegisteredYes("12345678"), JsPath \ "previouslyRegistered" \ "prevMLRRegNo"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("previouslyRegistered" -> true)

      Json.fromJson[PreviouslyRegistered](json) must
        be(JsError((JsPath \ "previouslyRegistered" \ "prevMLRRegNo") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(PreviouslyRegisteredNo) must
        be(Json.obj("previouslyRegistered" -> false))

      Json.toJson(PreviouslyRegisteredYes("12345678")) must
        be(Json.obj(
          "previouslyRegistered" -> true,
          "prevMLRRegNo" -> "12345678"
        ))
    }
  }
}
