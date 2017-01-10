package models.aboutthebusiness

import models.{Country, DateOfChange}
import models.businesscustomer.Address
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsNull, JsPath, JsSuccess, Json}

class RegisteredOfficeSpec extends PlaySpec with MockitoSugar {

  "RegisteredOffice" must {

    "successfully validate" when {
      "given a valid UK address" in {
        val ukModel = Map(
          "isUK" -> Seq("true"),
          "addressLine1" -> Seq("38B"),
          "addressLine2" -> Seq("building"),
          "addressLine3" -> Seq("street"),
          "addressLine4" -> Seq("Longbenton"),
          "postCode" -> Seq("NE7 7DX")
        )

        RegisteredOffice.formRule.validate(ukModel) must
          be(Success(RegisteredOfficeUK(
            "38B",
            "building",
            Some("street"),
            Some("Longbenton"),
            "NE7 7DX",
            None
          )))
      }

      "given a valid non UK address" in {
        val nonUKModel = Map(
          "isUK" -> Seq("false"),
          "addressLineNonUK1" -> Seq("38B"),
          "addressLineNonUK2" -> Seq("building"),
          "addressLineNonUK3" -> Seq("street"),
          "addressLineNonUK4" -> Seq("Area"),
          "country" -> Seq("GB")
        )

        RegisteredOffice.formRule.validate(nonUKModel) must
          be(Success(
            RegisteredOfficeNonUK(
              "38B",
              "building",
              Some("street"),
              Some("Area"),
              Country("United Kingdom", "GB"),
              None)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        RegisteredOffice.formRule.validate(Map.empty) must
          be(Failure(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.atb.registered.office.uk.or.overseas"))
          )))
      }

      "given missing data represented by an empty string" in {
        val data = Map(
          "isUK" -> Seq("")
        )

        RegisteredOffice.formRule.validate(data) must
          be(Failure(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.atb.registered.office.uk.or.overseas"))
          )))
      }

      "given an isUK value of 'false' and" when {
        "given missing data represented by an empty String" in {
          val data = Map(
            "isUK" -> Seq("false"),
            "addressLineNonUK1" -> Seq(""),
            "addressLineNonUK2" -> Seq(""),
            "country" -> Seq("")
          )

          RegisteredOffice.formRule.validate(data) must
            be(Failure(Seq(
              (Path \ "addressLineNonUK1") -> Seq(ValidationError("error.required.address.line1")),
              (Path \ "addressLineNonUK2") -> Seq(ValidationError("error.required.address.line2")),
              (Path \ "country") -> Seq(ValidationError("error.required.country"))
            )))
        }

        "given invalid data containing too many characters" in {
          val data = Map(
            "isUK" -> Seq("false"),
            "addressLineNonUK1" -> Seq("a" * 36),
            "addressLineNonUK2" -> Seq("a" * 36),
            "addressLineNonUK3" -> Seq("a" * 36),
            "addressLineNonUK4" -> Seq("a" * 36),
            "country" -> Seq("UK" * 12)
          )

          RegisteredOffice.formRule.validate(data) must
            be(Failure(Seq(
              (Path \ "addressLineNonUK1") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLineNonUK2") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLineNonUK3") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLineNonUK4") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "country") -> Seq(ValidationError("error.invalid.country"))
            )))
        }

      }

      "given an isUK value of 'true' and" when {
        "given missing data represented by an empty String" in {
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

        "given invalid data containing too many characters" in {
          val data = Map(
            "isUK" -> Seq("true"),
            "addressLine1" -> Seq("38B"),
            "addressLine2" -> Seq("a" * 36),
            "addressLine3" -> Seq("a" * 36),
            "addressLine4" -> Seq("a" * 36),
            "postCode" -> Seq("UK" * 12)
          )

          RegisteredOffice.formRule.validate(data) must
            be(Failure(Seq(
              (Path \ "addressLine2") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLine3") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLine4") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "postCode") -> Seq(ValidationError("error.invalid.postcode"))
            )))
        }

      }

    }


    "validate toLines for UK address" in {
      RegisteredOfficeUK("38B", "some street", None, None, "NE7 7ST").toLines must be(Seq("38B",
        "some street",
        "NE7 7ST"))

    }

    "validate toLines for Non UK address" in {
      RegisteredOfficeNonUK("38B", "some street", None, None, Country("United Kingdom", "GB")).toLines must be(Seq("38B",
        "some street",
        "United Kingdom"))

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

      val data = RegisteredOfficeNonUK("38B", "Some Street", None, None, Country("United Kingdom", "GB"))

      RegisteredOffice.formWrites.writes(data) mustBe Map("isUK" -> Seq("false"),
        "addressLineNonUK1" -> Seq("38B"),
        "addressLineNonUK2" -> Seq("Some Street"),
        "addressLineNonUK3" -> Seq(""),
        "addressLineNonUK4" -> Seq(""),
        "country" -> Seq("GB"))
    }

    "json read the given non UK address" in {

      val data = RegisteredOfficeUK("38B", "Longbenton", Some("line 1"), None, "NE7 7DX")
      val jsonObj = Json.obj("postCode" -> "NE7 7DX")

      Json.fromJson[RegisteredOffice](jsonObj) must be
      JsSuccess(data, JsPath \ "postCode")
    }

    "write correct value to json with date of change" in {
      val data = RegisteredOfficeUK("38B", "Longbenton", Some("line 1"), None, "NE7 7DX", Some(DateOfChange(new LocalDate(2017,1,1))))

      Json.toJson(data) must
        be(Json.obj(
          "addressLine1" -> "38B",
          "addressLine2" -> "Longbenton",
          "addressLine3" -> "line 1",
          "addressLine4" -> JsNull,
          "postCode" -> "NE7 7DX",
          "dateOfChange" -> "2017-01-01")
        )
    }

    "write correct value to json without date of change" in {
      val data = RegisteredOfficeUK("38B", "Longbenton", Some("line 1"), None, "NE7 7DX", None)

      Json.toJson(data) must
        be(Json.obj(
          "addressLine1" -> "38B",
          "addressLine2" -> "Longbenton",
          "addressLine3" -> "line 1",
          "addressLine4" -> JsNull,
          "postCode" -> "NE7 7DX",
          "dateOfChange" -> JsNull)
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
      Country("United Kingdom", "GB")
    )

    "Round trip a UK Address correctly through serialisation" in {
      RegisteredOffice.jsonReads.reads(
        RegisteredOffice.jsonWrites.writes(uKRegisteredOffice)
      ) must be(JsSuccess(uKRegisteredOffice))
    }

    "Round trip a Non UK Address correctly through serialisation" in {
      RegisteredOffice.jsonReads.reads(
        RegisteredOffice.jsonWrites.writes(nonUKRegisteredOffice)
      ) must be(JsSuccess(nonUKRegisteredOffice))
    }

    "convert Business Customer Address to RegisteredOfficeUK" in {
      val address = Address("addr1", "addr2", Some("line3"), Some("line4"), Some("NE2 6GH"), Country("United Kingdom", "GB"))

      RegisteredOffice.convert(address) must be(RegisteredOfficeUK("addr1", "addr2", Some("line3"), Some("line4"), "NE2 6GH"))
    }

    "convert Business Customer Address to RegisteredOfficeNonUK" in {
      val address = Address("addr1", "addr2", Some("line3"), Some("line4"), None, Country("United Kingdom", "GB"))

      RegisteredOffice.convert(address) must be(RegisteredOfficeNonUK("addr1", "addr2", Some("line3"), Some("line4"), Country("United Kingdom", "GB")))
    }
  }
}

