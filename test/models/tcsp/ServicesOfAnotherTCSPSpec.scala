package models.tcsp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Failure, Path, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ServicesOfAnotherTCSPSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given enum value" in {
      ServicesOfAnotherTCSP.formRule.validate(Map("servicesOfAnotherTCSP" -> Seq("false"))) must
        be(Success(ServicesOfAnotherTCSPNo))
    }

    "successfully validate given an `Yes` value" in {

      val data = Map(
        "servicesOfAnotherTCSP" -> Seq("true"),
        "mlrRefNumber" -> Seq("12345678")
      )

      ServicesOfAnotherTCSP.formRule.validate(data) must
        be(Success(ServicesOfAnotherTCSPYes("12345678")))
    }

    "fail when mandatory fields are missing" in {
      ServicesOfAnotherTCSP.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "servicesOfAnotherTCSP") -> Seq(ValidationError("error.required.tcsp.services.another.tcsp"))
        )))

    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "servicesOfAnotherTCSP" -> Seq("true"),
        "mlrRefNumber" -> Seq("")
      )

      ServicesOfAnotherTCSP.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "mlrRefNumber") -> Seq(ValidationError("error.required.tcsp.mlr.reference.number"))
        )))
    }

    "fail to validate given an `Yes` with invalid value" in {

      val data = Map(
        "servicesOfAnotherTCSP" -> Seq("true"),
        "mlrRefNumber" -> Seq("123qed")
      )

      ServicesOfAnotherTCSP.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "mlrRefNumber") -> Seq(ValidationError("error.invalid.tcsp.mlr.reference.number"))
        )))
    }

    "write correct data from enum value" in {

      ServicesOfAnotherTCSP.formWrites.writes(ServicesOfAnotherTCSPNo) must
        be(Map("servicesOfAnotherTCSP" -> Seq("false")))

    }

    "write correct data from `yes` value" in {

      ServicesOfAnotherTCSP.formWrites.writes(ServicesOfAnotherTCSPYes("12345678")) must
        be(Map("servicesOfAnotherTCSP" -> Seq("true"), "mlrRefNumber" -> Seq("12345678")))

    }

    "JSON validation" must {

      "successfully validate given an enum value" in {

        Json.fromJson[ServicesOfAnotherTCSP](Json.obj("servicesOfAnotherTCSP" -> false)) must
          be(JsSuccess(ServicesOfAnotherTCSPNo, JsPath \ "servicesOfAnotherTCSP"))
      }

      "successfully validate given an `Yes` value" in {

        Json.fromJson[ServicesOfAnotherTCSP](Json.obj("servicesOfAnotherTCSP" -> true, "mlrRefNumber" -> "12345678")) must
          be(JsSuccess(ServicesOfAnotherTCSPYes("12345678"), JsPath \ "servicesOfAnotherTCSP" \ "mlrRefNumber"))
      }

      "fail to validate when given an empty `Yes` value" in {

        val json = Json.obj("servicesOfAnotherTCSP" -> true)

        Json.fromJson[ServicesOfAnotherTCSP](json) must
          be(JsError((JsPath \ "servicesOfAnotherTCSP" \ "mlrRefNumber") -> ValidationError("error.path.missing")))
      }

      "write the correct value" in {

        Json.toJson(ServicesOfAnotherTCSPNo) must
          be(Json.obj("servicesOfAnotherTCSP" -> false))

        Json.toJson(ServicesOfAnotherTCSPYes("12345678")) must
          be(Json.obj(
            "servicesOfAnotherTCSP" -> true,
            "mlrRefNumber" -> "12345678"
          ))
      }

    }


  }

}
