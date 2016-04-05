package models.businessactivities

import models.{Country, FormTypes}
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Success, Failure, Path}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsSuccess, Json}

class AccountantsAddressSpec extends PlaySpec {

  val DefaultAddressLine1 = "Default Line 1"
  val DefaultAddressLine2 = "Default Line 2"
  val DefaultAddressLine3 = Some("Default Line 3")
  val DefaultAddressLine4 = Some("Default Line 4")
  val DefaultPostcode = "NE1 7YX"
  val DefaultCountry = Country("United Kingdom", "GB")

  val NewAddressLine1 = "New Line 1"
  val NewAddressLine2 = "New Line 2"
  val NewAddressLine3 = Some("New Line 3")
  val NewAddressLine4 = Some("New Line 4")
  val NewPostcode = "CR6 5HG"

  val DefaultUKAddress = UkAccountantsAddress(DefaultAddressLine1,
    DefaultAddressLine2,
    DefaultAddressLine3,
    DefaultAddressLine4,
    DefaultPostcode)

  val DefaultNonUKAddress = NonUkAccountantsAddress(DefaultAddressLine1,
    DefaultAddressLine2,
    DefaultAddressLine3,
    DefaultAddressLine4,
    DefaultCountry)

  val DefaultUKJson = Json.obj(
    "accountantsAddressLine1" -> DefaultAddressLine1,
    "accountantsAddressLine2" -> DefaultAddressLine2,
    "accountantsAddressLine3" -> DefaultAddressLine3,
    "accountantsAddressLine4" -> DefaultAddressLine4,
    "accountantsAddressPostCode" -> DefaultPostcode
  )

  val DefaultNonUKJson = Json.obj(
    "accountantsAddressLine1" -> DefaultAddressLine1,
    "accountantsAddressLine2" -> DefaultAddressLine2,
    "accountantsAddressLine3" -> DefaultAddressLine3,
    "accountantsAddressLine4" -> DefaultAddressLine4,
    "accountantsAddressCountry" -> DefaultCountry.code
  )

  val DefaultUKModel = Map(
    "isUK" -> Seq("true"),
    "addressLine1" -> Seq(DefaultAddressLine1),
    "addressLine2" -> Seq(DefaultAddressLine2),
    "addressLine3" -> Seq("Default Line 3"),
    "addressLine4" -> Seq("Default Line 4"),
    "postCode" -> Seq(DefaultPostcode)
  )

  val DefaultNonUKModel = Map(
    "isUK" -> Seq("false"),
    "addressLine1" -> Seq(DefaultAddressLine1),
    "addressLine2" -> Seq(DefaultAddressLine2),
    "addressLine3" -> Seq("Default Line 3"),
    "addressLine4" -> Seq("Default Line 4"),
    "country" -> Seq(DefaultCountry.code)
  )


  "AccountantsAddress" must {

    "validate toLines for UK address" in {
      DefaultUKAddress.toLines must be (Seq("Default Line 1",
        "Default Line 2",
        "Default Line 3",
        "Default Line 4",
        "NE1 7YX"))
    }

    "validate toLines for Non UK address" in {
      DefaultNonUKAddress.toLines must be (Seq("Default Line 1",
        "Default Line 2",
        "Default Line 3",
        "Default Line 4",
        "United Kingdom"))
    }

    "Form validation" must {
      "Read UK Address" in {
        AccountantsAddress.formRule.validate(DefaultUKModel) must be (Success(DefaultUKAddress))
      }

      "throw error when mandatory fields are missing" in {
        AccountantsAddress.formRule.validate(Map.empty) must be(
          Failure(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.uk.or.overseas"))
          )))
      }

      "throw error when there is an invalid data" in {
        val model =  DefaultNonUKModel ++ Map("isUK" -> Seq("HGHHHH"))
        AccountantsAddress.formRule.validate(model) must be(
          Failure(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.uk.or.overseas"))
          )))
      }

      "throw error when length of country exceeds max length" in {
        val model =  DefaultNonUKModel ++ Map("country" -> Seq("HGHHHH"))
        AccountantsAddress.formRule.validate(model) must be(
          Failure(Seq(
            (Path \ "country") -> Seq(ValidationError("error.invalid.country"))
          )))
      }

      "Read Non UK Address" in {
        AccountantsAddress.formRule.validate(DefaultNonUKModel) must be (Success(DefaultNonUKAddress))
      }

    }

    "JSON validation" must {

      "Round trip a UK Address correctly" in {
        AccountantsAddress.jsonReads.reads(
          AccountantsAddress.jsonWrites.writes(DefaultUKAddress)
        ) must be (JsSuccess(DefaultUKAddress))
      }

      "Round trip a Non UK Address correctly" in {
        AccountantsAddress.jsonReads.reads(
          AccountantsAddress.jsonWrites.writes(DefaultNonUKAddress)
        ) must be (JsSuccess(DefaultNonUKAddress))
      }

      "Serialise UK address as expected" in {
        Json.toJson(DefaultUKAddress) must be(DefaultUKJson)
      }

      "Serialise non-UK address as expected" in {
        Json.toJson(DefaultNonUKAddress) must be(DefaultNonUKJson)
      }

      "Deserialise UK address as expected" in {
        DefaultUKJson.as[AccountantsAddress] must be(DefaultUKAddress)
      }

      "Deserialise non-UK address as expected" in {
        DefaultNonUKJson.as[AccountantsAddress] must be(DefaultNonUKAddress)
      }
    }
  }
}
