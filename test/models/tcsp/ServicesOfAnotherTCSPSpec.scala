package models.tcsp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ServicesOfAnotherTCSPSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given enum value" in {
      ServicesOfAnotherTCSP.formRule.validate(Map("servicesOfAnotherTCSP" -> Seq("false"))) must
        be(Valid(ServicesOfAnotherTCSPNo))
    }

    "successfully validate given an `Yes` value" in {

      val data = Map(
        "servicesOfAnotherTCSP" -> Seq("true"),
        "mlrRefNumber" -> Seq("12345678")
      )

      ServicesOfAnotherTCSP.formRule.validate(data) must
        be(Valid(ServicesOfAnotherTCSPYes("12345678")))
    }

    "successfully validate given an alphanumeric mlr number" in {

      val data = Map(
        "servicesOfAnotherTCSP" -> Seq("true"),
        "mlrRefNumber" -> Seq("i9w9834ubkid89n")
      )

      ServicesOfAnotherTCSP.formRule.validate(data) must be {
        Valid(ServicesOfAnotherTCSPYes("i9w9834ubkid89n"))
      }

    }

    "fail when mandatory fields are missing" in {
      ServicesOfAnotherTCSP.formRule.validate(Map.empty) must
        be(Invalid(Seq(
          (Path \ "servicesOfAnotherTCSP") -> Seq(ValidationError("error.required.tcsp.services.another.tcsp"))
        )))

    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "servicesOfAnotherTCSP" -> Seq("true"),
        "mlrRefNumber" -> Seq("")
      )

      ServicesOfAnotherTCSP.formRule.validate(data) must
        be(Invalid(Seq(
          (Path \ "mlrRefNumber") -> Seq(ValidationError("error.required.tcsp.mlr.reference.number"))
        )))
    }

    "fail to validate given an `Yes` with invalid value" in {

      val data = Map(
        "servicesOfAnotherTCSP" -> Seq("true"),
        "mlrRefNumber" -> Seq("123qed")
      )

      ServicesOfAnotherTCSP.formRule.validate(data) must be(
        Invalid(Seq((Path \ "mlrRefNumber") -> Seq(ValidationError("error.invalid.tcsp.mlr.reference.number"))
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
      import play.api.data.validation.ValidationError

      "successfully validate given an enum value" in {

        Json.fromJson[ServicesOfAnotherTCSP](Json.obj("servicesOfAnotherTCSP" -> false)) must
          be(JsSuccess(ServicesOfAnotherTCSPNo, JsPath ))
      }

      "successfully validate given an `Yes` value" in {

        Json.fromJson[ServicesOfAnotherTCSP](Json.obj("servicesOfAnotherTCSP" -> true, "mlrRefNumber" -> "12345678")) must
          be(JsSuccess(ServicesOfAnotherTCSPYes("12345678"), JsPath \ "mlrRefNumber"))
      }

      "fail to validate when given an empty `Yes` value" in {

        val json = Json.obj("servicesOfAnotherTCSP" -> true)

        Json.fromJson[ServicesOfAnotherTCSP](json) must
          be(JsError((JsPath \ "mlrRefNumber") -> ValidationError("error.path.missing")))
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
