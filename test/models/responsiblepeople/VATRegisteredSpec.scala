package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class VATRegisteredSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {


    "successfully validate given an enum value" in {

      VATRegistered.formRule.validate(Map("registeredForVAT" -> Seq("false"))) must
        be(Valid(VATRegisteredNo))
    }

    "successfully validate given an `Yes` value" in {

      val data = Map(
        "registeredForVAT" -> Seq("true"),
        "vrnNumber" -> Seq("123456789")
      )

      VATRegistered.formRule.validate(data) must
        be(Valid(VATRegisteredYes("123456789")))
    }


    "fail to validate given missing mandatory field" in {

      VATRegistered.formRule.validate(Map.empty) must
        be(Invalid(Seq(
          (Path \ "registeredForVAT") -> Seq(ValidationError("error.required.atb.registered.for.vat"))
        )))
    }

    "fail to validate given invalid field" in {

      VATRegistered.formRule.validate(Map.empty) must
        be(Invalid(Seq(
          (Path \ "registeredForVAT") -> Seq(ValidationError("error.required.atb.registered.for.vat"))
        )))
    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "registeredForVAT" -> Seq("true"),
        "vrnNumber" -> Seq("")
      )

      VATRegistered.formRule.validate(data) must
        be(Invalid(Seq(
          (Path \ "vrnNumber") -> Seq(ValidationError("error.required.vat.number"))
        )))
    }

    "write correct data from enum value" in {

      VATRegistered.formWrites.writes(VATRegisteredNo) must
        be(Map("registeredForVAT" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      VATRegistered.formWrites.writes(VATRegisteredYes("12345678")) must
        be(Map("registeredForVAT" -> Seq("true"), "vrnNumber" -> Seq("12345678")))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[VATRegistered](Json.obj("registeredForVAT" -> false)) must
        be(JsSuccess(VATRegisteredNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("registeredForVAT" -> true, "vrnNumber" ->"12345678")

      Json.fromJson[VATRegistered](json) must
        be(JsSuccess(VATRegisteredYes("12345678"), JsPath \ "vrnNumber"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("registeredForVAT" -> true)

      Json.fromJson[VATRegistered](json) must
        be(JsError((JsPath \ "vrnNumber") -> play.api.data.validation.ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(VATRegisteredNo) must
        be(Json.obj("registeredForVAT" -> false))

      Json.toJson(VATRegisteredYes("12345678")) must
        be(Json.obj(
          "registeredForVAT" -> true,
          "vrnNumber" -> "12345678"
        ))
    }
  }


}
