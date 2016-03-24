package models.aboutthebusiness

import models.FormTypes
import models.businesscustomer.Address
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, JsNull, Json}

class RegisteredOfficeSpec extends PlaySpec with MockitoSugar {

  "RegisteredOffice" must {

    "validate the given UK address" in {
      val ukModel = Map(
        "isUK" -> Seq("true"),
        "addressLine1" -> Seq("38B"),
        "addressLine2" -> Seq("building"),
        "addressLine3" -> Seq("street"),
        "addressLine4" -> Seq("Longbenton"),
        "postCode" -> Seq("NE7 7DX")
      )

      RegisteredOffice.formRule.validate(ukModel) must
        be(Success(RegisteredOfficeUK("38B", "building", Some("street"), Some("Longbenton"), "NE7 7DX")))
    }

    "validate the given non UK address" in {
      val nonUKModel = Map(
        "isUK" -> Seq("false"),
        "addressLineNonUK1" -> Seq("38B"),
        "addressLineNonUK2" -> Seq("building"),
        "addressLineNonUK3" -> Seq("street"),
        "addressLineNonUK4" -> Seq("Area"),
        "country" -> Seq("MN")
      )

      RegisteredOffice.formRule.validate(nonUKModel) must
        be(Success(RegisteredOfficeNonUK("38B", "building", Some("street"), Some("Area"), "MN")))
    }

    "validate toLines for UK address" in {
      RegisteredOfficeUK("38B", "some street", None, None, "NE7 7ST").toLines must be (Seq("38B",
        "some street",
        "NE7 7ST"))

    }

    "validate toLines for Non UK address" in {
      RegisteredOfficeNonUK("38B", "some street", None, None, "AR").toLines must be (Seq("38B",
        "some street",
        "AR"))

    }

    "fail to validation for missing mandatory field" in {

      RegisteredOffice.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "isUK") -> Seq(ValidationError("error.required.atb.confirm.office"))
        )))
    }

    "fail to validation for not filling mandatory field" in {
      val data = Map(
        "isUK" -> Seq("true"),
        "addressLine1" -> Seq(""),
        "addressLine2" -> Seq(""),
        "postCode" -> Seq("")
      )

      RegisteredOffice.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "addressLine1") -> Seq(ValidationError("error.required.address.line1")),
          (Path \ "addressLine2") -> Seq(ValidationError("error.required.address.line2")),
          (Path \ "postCode") -> Seq(ValidationError("error.required.postcode"))
        )))
    }

    "fail to validation for invalid model" in {
      val data = Map(
        "isUK" -> Seq("true"),
        "addressLine1" -> Seq("38B"),
        "addressLine2" -> Seq("a"*36),
        "addressLine3" -> Seq("a"*36),
        "addressLine4" -> Seq("a"*36),
        "postCode" -> Seq("UK"*12)
      )

      RegisteredOffice.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "addressLine2") -> Seq(ValidationError("error.max.length.address.line", FormTypes.maxAddressLength)),
          (Path \ "addressLine3") -> Seq(ValidationError("error.max.length.address.line", FormTypes.maxAddressLength)),
          (Path \ "addressLine4") -> Seq(ValidationError("error.max.length.address.line", FormTypes.maxAddressLength)),
          (Path \ "postCode") -> Seq(ValidationError("error.invalid.postcode", FormTypes.maxPostCodeTypeLength))
        )))
    }

    "write correct UK address to the model" in {

      val data = RegisteredOfficeUK("38B", "Some building", Some("street"), Some("Longbenton"), "NE7 7DX")

      RegisteredOffice.formWrites.writes(data) mustBe Map("isUK" -> Seq("true"),
                                                          "addressLine1" -> Seq("38B"),
                                                          "addressLine2" -> Seq("Some building"),
                                                          "addressLine3" -> Seq("street"),
                                                          "addressLine4" -> Seq("Longbenton"),
                                                          "postCode" -> Seq("NE7 7DX"))
    }

    "write correct Non UK address to the model" in {

      val data = RegisteredOfficeNonUK("38B", "Some Street", None, None, "MN")

      RegisteredOffice.formWrites.writes(data) mustBe  Map("isUK" -> Seq("false"),
                                                            "addressLineNonUK1" -> Seq("38B"),
                                                            "addressLineNonUK2" -> Seq("Some Street"),
                                                            "addressLineNonUK3" -> Seq(""),
                                                            "addressLineNonUK4" -> Seq(""),
                                                            "country" -> Seq("MN"))
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

    "covert Business Customer Address to RegisteredOfficeUK" in {
      val address = Address("addr1", "addr2", Some("line3"), Some("line4"), Some("NE2 6GH"), "GB")

      RegisteredOffice.convert(address) must be(RegisteredOfficeUK("addr1","addr2",Some("line3"),Some("line4"),"NE2 6GH"))
    }

    "covert Business Customer Address to RegisteredOfficeNonUK" in {
      val address = Address("addr1", "addr2", Some("line3"), Some("line4"),None, "HP")

      RegisteredOffice.convert(address) must be(RegisteredOfficeNonUK("addr1","addr2",Some("line3"),Some("line4"),"HP"))
    }
  }
}

