package models.aboutthebusiness

import models.FormTypes
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, JsNull, Json}

class RegisteredOfficeSpec extends PlaySpec with MockitoSugar {

  "RegisteredOfficeOrMainPlaceOfBusiness" must {

    "validate the given UK address" in {
      val model = Map(
        "isUK" -> Seq("true"),
        "addressLine1" -> Seq("38B"),
        "addressLine2" -> Seq("Longbenton"),
        "addressLine3" -> Seq(""),
        "addressLine4" -> Seq(""),
        "postCode" -> Seq("NE7 7DX")
      )

      RegisteredOffice.formRule.validate(model) must
        be(Success(RegisteredOfficeUK("38B", "Longbenton", None, None, "NE7 7DX")))
    }

    "validate the given non UK address" in {
      val model = Map(
        "isUK" -> Seq("false"),
        "addressLineNonUK1" -> Seq("38B"),
        "addressLineNonUK2" -> Seq("Longbenton"),
        "addressLineNonUK3" -> Seq(""),
        "addressLineNonUK4" -> Seq(""),
        "country" -> Seq("UK")
      )

      RegisteredOffice.formRule.validate(model) must
        be(Success(RegisteredOfficeNonUK("38B", "Longbenton", None, None, "UK")))
    }

    "fail to validation for not filling mandatory field" in {
      val data = Map(
        "isUK" -> Seq("false"),
        "addressLineNonUK2" -> Seq("Longbenton"),
        "addressLineNonUK3" -> Seq(""),
        "addressLineNonUK4" -> Seq(""),
        "postCode" -> Seq("UK")
      )

      RegisteredOffice.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "addressLineNonUK1") -> Seq(ValidationError("error.required")),
          (Path \ "country") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validation for not filling mandatory isUKOrOverseas field" in {
      val data = Map(
        "addressLineNonUK2" -> Seq(""),
        "addressLineNonUK3" -> Seq(""),
        "addressLineNonUK4" -> Seq(""),
        "country" -> Seq("")
      )

      RegisteredOffice.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "isUK") -> Seq(ValidationError("error.required"))

        )))
    }

    "fail to validation for invalid model" in {
      val data = Map(
        "isUK" -> Seq("true"),
        "addressLine1" -> Seq("38B"),
        "addressLine2" -> Seq("a"*36),
        "addressLine3" -> Seq(""),
        "addressLine4" -> Seq(""),
        "postCode" -> Seq("UK"*12)
      )

      RegisteredOffice.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "addressLine2") -> Seq(ValidationError("error.maxLength", FormTypes.maxAddressLength)),
          (Path \ "postCode") -> Seq(ValidationError("error.maxLength", FormTypes.maxPostCodeTypeLength))
        )))
    }

    "write correct data to the model " in {

      val data = RegisteredOfficeUK("38B", "Longbenton", None, None, "NE7 7DX")

      RegisteredOffice.formWrites.writes(data) must be
      Map("addressLine1" -> "38B",
        "addressLine2" -> "Longbenton",
        "addressLine3" -> "",
        "addressLine4" -> "",
        "postCode" -> "NE7 &DX")
    }

    "json read the given non UK address" in {

      val data = RegisteredOfficeUK("38B", "Longbenton", Some("line 1"), None, "NE7 7DX")
      val jsonObj = Json.obj("postCode" -> "NE7 7DX")

      Json.fromJson[RegisteredOffice](jsonObj) must be
      JsSuccess(data, JsPath \ "postCode")
    }

    "write correct value to json" in {
      val data = RegisteredOfficeUK("38B", "Longbenton", Some("line 1"), None, "NE7 7DX")

      Json.toJson(data) must
        be( Json.obj(
          "addressLine1" -> "38B",
          "addressLine2" -> "Longbenton",
          "addressLine3" -> "line 1",
          "addressLine4" -> JsNull,
          "postCode" -> "NE7 7DX")
        )
    }

    val uKRegisteredOffice = RegisteredOfficeUK(
      "Test Address 1",
      "Test Address 2",
      Some("Test Address 3"),
      Some("Test Address 4"),
      "P05TC0DE"
    )

    val nonUKRegisteredOffice = RegisteredOfficeNonUK(
      "Test Address 1",
      "Test Address 2",
      Some("Test Address 3"),
      Some("Test Address 4"),
      "Country"
    )

    "Round trip a UK Address correctly through serialisation" in {
      RegisteredOffice.jsonReads.reads(
        RegisteredOffice.jsonWrites.writes(uKRegisteredOffice)
      ) must be (JsSuccess(uKRegisteredOffice))
    }

    "Round trip a Non UK Address correctly through serialisation" in {
      RegisteredOffice.jsonReads.reads(
        RegisteredOffice.jsonWrites.writes(nonUKRegisteredOffice)
      ) must be (JsSuccess(nonUKRegisteredOffice))
    }
  }
}

