package models.aboutthebusiness

import models.FormTypes
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, JsNull, Json}

class WhereIsRegOfficeOrMainPlaceOfBusinessSpec extends PlaySpec with MockitoSugar {

  "WhereIsRegOfficeOrMainPlaceOfBusiness" must {

    "validate the given UK address" in {
      val model = Map(
        "isUKOrOverseas" -> Seq("true"),
        "addressLine1" -> Seq("38B"),
        "addressLine2" -> Seq("Longbenton"),
        "addressLine3" -> Seq(""),
        "addressLine4" -> Seq(""),
        "postCode" -> Seq("NE7 7DX")
      )

      WhereIsRegOfficeOrMainPlaceOfBusiness.formRule.validate(model) must
        be(Success(RegOfficeOrMainPlaceOfBusinessUK("38B", "Longbenton", None, None, "NE7 7DX")))
    }

    "validate the given non UK address" in {
      val model = Map(
        "isUKOrOverseas" -> Seq("false"),
        "addressLine1" -> Seq("38B"),
        "addressLine2" -> Seq("Longbenton"),
        "addressLine3" -> Seq(""),
        "addressLine4" -> Seq(""),
        "country" -> Seq("UK")
      )

      WhereIsRegOfficeOrMainPlaceOfBusiness.formRule.validate(model) must
        be(Success(RegOfficeOrMainPlaceOfBusinessNonUK("38B", "Longbenton", None, None, "UK")))
    }

    "fail to validation for not filling mandatory field" in {
      val data = Map(
        "isUKOrOverseas" -> Seq("true"),
        "addressLine2" -> Seq("Longbenton"),
        "addressLine3" -> Seq(""),
        "addressLine4" -> Seq(""),
        "country" -> Seq("UK")
      )

      WhereIsRegOfficeOrMainPlaceOfBusiness.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "addressLine1") -> Seq(ValidationError("error.required")),
          (Path \ "postCode") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validation for invalid model" in {
      val data = Map(
        "isUKOrOverseas" -> Seq("true"),
        "addressLine1" -> Seq("38B"),
        "addressLine2" -> Seq("a"*36),
        "addressLine3" -> Seq(""),
        "addressLine4" -> Seq(""),
        "postCode" -> Seq("UK"*12)
      )

      WhereIsRegOfficeOrMainPlaceOfBusiness.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "addressLine2") -> Seq(ValidationError("error.maxLength", FormTypes.maxAddressLength)),
          (Path \ "postCode") -> Seq(ValidationError("error.maxLength", FormTypes.maxPostCodeLength))
        )))
    }

    "write correct data to the model " in {

      val data = RegOfficeOrMainPlaceOfBusinessNonUK("38B", "Longbenton", None, None, "NE7 7DX")

      WhereIsRegOfficeOrMainPlaceOfBusiness.formWrites.writes(data) must be
      Map("addressLine1" -> "38B",
        "addressLine2" -> "Longbenton",
        "addressLine3" -> "",
        "addressLine4" -> "",
        "postCode" -> "NE7 &DX")
    }

    "json read the given non UK address" in {

      val data = RegOfficeOrMainPlaceOfBusinessUK("38B", "Longbenton", Some("line 1"), None, "NE7 7DX")
      val jsonObj = Json.obj("postCode" -> "NE7 7DX")

      Json.fromJson[WhereIsRegOfficeOrMainPlaceOfBusiness](jsonObj) must be
      JsSuccess(data, JsPath \ "postCode")
    }

    "write correct value to json" in {
      val data = RegOfficeOrMainPlaceOfBusinessUK("38B", "Longbenton", Some("line 1"), None, "NE7 7DX")

      Json.toJson(data) must
        be( Json.obj("addressLine1" -> "38B",
          "addressLine2" -> "Longbenton",
          "addressLine3" -> "line 1",
          "addressLine4" -> JsNull,
          "postCode" -> "NE7 7DX")
        )

    }
  }
}



