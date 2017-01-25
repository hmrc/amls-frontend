package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Failure, Path, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class SaRegisteredSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "utrType" must {

      "successfully validate" in {

        SaRegistered.utrType.validate("1234567890") must
          be(Success("1234567890"))
      }

      "fail to validate an empty string" in {

        SaRegistered.utrType.validate("") must
          be(Failure(Seq(
            Path -> Seq(ValidationError("error.required.utr.number"))
          )))
      }

      "fail to validate a string longer than 10" in {

        SaRegistered.utrType.validate("1" * 11) must
          be(Failure(Seq(
            Path -> Seq(ValidationError("error.invalid.length.utr.number"))
          )))
      }
    }

    "successfully validate given an enum value" in {
      SaRegistered.formRule.validate(Map("saRegistered" -> Seq("false"))) must
        be(Success(SaRegisteredNo))
    }

    "successfully validate given an `Yes` value" in {
      val data = Map(
        "saRegistered" -> Seq("true"),
        "utrNumber" -> Seq("0123456789")
      )

      SaRegistered.formRule.validate(data) must
        be(Success(SaRegisteredYes("0123456789")))
    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "saRegistered" -> Seq("true")
      )

      SaRegistered.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "utrNumber") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data from enum value" in {

      SaRegistered.formWrites.writes(SaRegisteredNo) must
        be(Map("saRegistered" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      SaRegistered.formWrites.writes(SaRegisteredYes("0123456789")) must
        be(Map("saRegistered" -> Seq("true"), "utrNumber" -> Seq("0123456789")))
    }

  }

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[SaRegistered](Json.obj("saRegistered" -> false)) must
        be(JsSuccess(SaRegisteredNo, JsPath \ "saRegistered"))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("saRegistered" -> true, "utrNumber" ->"0123456789")

      Json.fromJson[SaRegistered](json) must
        be(JsSuccess(SaRegisteredYes("0123456789"), JsPath \ "saRegistered" \ "utrNumber"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("saRegistered" -> true)

      Json.fromJson[SaRegistered](json) must
        be(JsError((JsPath \ "saRegistered" \ "utrNumber") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(SaRegisteredNo) must
        be(Json.obj("saRegistered" -> false))

      Json.toJson(SaRegisteredYes("0123456789")) must
        be(Json.obj(
          "saRegistered" -> true,
          "utrNumber" -> "0123456789"
        ))
    }
  }

}
