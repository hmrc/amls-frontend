package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Failure, Path, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class PreviouslyRegisteredSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate" when {
      "successfully validate given a 'no' value" in {

        PreviouslyRegistered.formRule.validate(Map("previouslyRegistered" -> Seq("false"))) must
          be(Success(PreviouslyRegisteredNo))
      }

      "successfully validate given an `Yes` value with 8 characters" in {

        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("1" * 8)
        )

        PreviouslyRegistered.formRule.validate(data) must
          be(Success(PreviouslyRegisteredYes("1" * 8)))
      }

      "successfully validate given an `Yes` value with 15 characters" in {

        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("1" * 15)
        )

        PreviouslyRegistered.formRule.validate(data) must
          be(Success(PreviouslyRegisteredYes("1" * 15)))
      }
    }

    "fail validation" when {

      "given a 'yes' value with more than 15 characters" in {
        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("1" * 20)
        )

        be(Failure(Seq(
          (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.invalid.atb.mlr.number"))
        )))
      }
      "given a 'yes' value with less than 8 characters" in {
        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("1" * 5)
        )

        be(Failure(Seq(
          (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.invalid.atb.mlr.number"))
        )))
      }
      "given a 'yes' value with between 9 and 14 characters" in {
        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("1" * 11)
        )

        be(Failure(Seq(
          (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.invalid.atb.mlr.number"))
        )))
      }
      "given a 'yes' value non-numeric characters" in {
        val data = Map(
          "previouslyRegistered" -> Seq("true"),
          "prevMLRRegNo" -> Seq("1ghy7cnj&")
        )

        be(Failure(Seq(
          (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.invalid.atb.mlr.number"))
        )))
      }

      "given an empty map" in {

        PreviouslyRegistered.formRule.validate(Map.empty) must
          be(Failure(Seq(
            (Path \ "previouslyRegistered") -> Seq(ValidationError("error.required.atb.previously.registered"))
          )))
      }

      "'Yes' is selected but no value is provided" when {
        "represented by an empty string" in {
          val data = Map(
            "previouslyRegistered" -> Seq("true"),
            "prevMLRRegNo" -> Seq("")
          )

          PreviouslyRegistered.formRule.validate(data) must
            be(Failure(Seq(
              (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.required.atb.mlr.number"))
            )))
        }

        "represented by a sequence of whitespace" in {
          val data = Map(
            "previouslyRegistered" -> Seq("true"),
            "prevMLRRegNo" -> Seq("       \t")
          )

          PreviouslyRegistered.formRule.validate(data) must
            be(Failure(Seq(
              (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.invalid.atb.mlr.number"))
            )))
        }

        "represented by a missing field" in {
          val data = Map(
            "previouslyRegistered" -> Seq("true")
          )

          PreviouslyRegistered.formRule.validate(data) must
            be(Failure(Seq(
              (Path \ "prevMLRRegNo") -> Seq(ValidationError("error.required"))
            )))
        }
      }

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

      val json = Json.obj("previouslyRegistered" -> true, "prevMLRRegNo" -> "12345678")

      Json.fromJson[PreviouslyRegistered](json) must
        be(JsSuccess(PreviouslyRegisteredYes("12345678"), JsPath \ "previouslyRegistered" \ "prevMLRRegNo"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("previouslyRegistered" -> true)

      Json.fromJson[PreviouslyRegistered](json) must
        be(JsError((JsPath \ "previouslyRegistered" \ "prevMLRRegNo") -> play.api.data.validation.ValidationError("error.path.missing")))
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
