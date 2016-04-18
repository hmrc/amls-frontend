package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class TrainingSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "information" must {

      "successfully validate" in {

        Training.informationType.validate("did the training for the anti money laundering. Dont know when") must
          be(Success("did the training for the anti money laundering. Dont know when"))
      }

      "fail to validate an empty string" in {

        Training.informationType.validate("") must
          be(Failure(Seq(
            Path -> Seq(ValidationError("error.required.rp.training.information"))
          )))
      }

      "fail to validate a string longer than 255 characters" in {

        Training.informationType.validate("A" * 256) must
          be(Failure(Seq(
            Path -> Seq(ValidationError("error.invalid.length.rp.training.information"))
          )))
      }
    }

    "successfully validate given an enum value" in {
      Training.formRule.validate(Map("training" -> Seq("false"))) must
        be(Success(TrainingNo))
    }

    "successfully validate given an `Yes` value" in {
      val data = Map(
        "training" -> Seq("true"),
        "information" -> Seq("0123456789")
      )

      Training.formRule.validate(data) must be(Success(TrainingYes("0123456789")))
    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "training" -> Seq("true")
      )

      Training.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "information") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data from enum value" in {

      Training.formWrites.writes(TrainingNo) must be(Map("training" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      Training.formWrites.writes(TrainingYes("0123456789")) must
        be(Map("training" -> Seq("true"), "information" -> Seq("0123456789")))
    }

  }

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[Training](Json.obj("training" -> false)) must
        be(JsSuccess(TrainingNo, JsPath \ "training"))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("training" -> true, "information" -> "0123456789")

      Json.fromJson[Training](json) must
        be(JsSuccess(TrainingYes("0123456789"), JsPath \ "training" \ "information"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("training" -> true)

      Json.fromJson[Training](json) must
        be(JsError((JsPath \ "training" \ "information") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(TrainingNo) must be(Json.obj("training" -> false))

      Json.toJson(TrainingYes("0123456789")) must
        be(Json.obj(
          "training" -> true,
          "information" -> "0123456789"
        ))
    }
  }

}
