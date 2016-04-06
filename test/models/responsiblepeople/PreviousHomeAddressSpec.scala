package models.responsiblepeople

import models.Country
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsSuccess, Json}

class PreviousHomeAddressSpec extends PlaySpec with MockitoSugar {

  val DefaultAddressLine1 = "Default Line 1"
  val DefaultAddressLine2 = "Default Line 2"
  val DefaultAddressLine3 = None
  val DefaultAddressLine4 = None
  val DefaultPostcode = "NE1 7YX"
  val DefaultForeignCountry = Country("Spain", "ES")
  val DefaultTimeAtAddress = ZeroToFiveMonths

  val ValidUKObject = PreviousHomeAddressUK(
    DefaultAddressLine1,
    DefaultAddressLine2,
    DefaultAddressLine3,
    DefaultAddressLine4,
    DefaultPostcode,
    DefaultTimeAtAddress)

  val ValidNonUKObject = PreviousHomeAddressNonUK(
    DefaultAddressLine1,
    DefaultAddressLine2,
    DefaultAddressLine3,
    DefaultAddressLine4,
    DefaultForeignCountry,
    DefaultTimeAtAddress)

  "Form Rules and Writes" must {

    val ValidUKForm = Map(
      "isUK" -> Seq("true"),
      "addressLine1" -> Seq(DefaultAddressLine1),
      "addressLine2" -> Seq(DefaultAddressLine2),
      "postCode" -> Seq(DefaultPostcode),
      "timeAtAddress" -> Seq("01")
    )

    val ValidNonUKForm = Map(
      "isUK" -> Seq("false"),
      "addressLineNonUK1" -> Seq(DefaultAddressLine1),
      "addressLineNonUK2" -> Seq(DefaultAddressLine2),
      "country" -> Seq("ES"),
      "timeAtAddress" -> Seq("01")
    )

    "successfully validate to a UK object given correct fields" in {
      PreviousHomeAddress.formRule.validate(ValidUKForm) must be(Success(ValidUKObject))
    }

    "successfully validate to a non-UK object given correct fields" in {
      PreviousHomeAddress.formRule.validate(ValidNonUKForm) must be(Success(ValidNonUKObject))
    }

    "throw error when mandatory isUK field is missing" in {
      PreviousHomeAddress.formRule.validate(Map.empty) must be(
        Failure(Seq(
          (Path \ "isUK") -> Seq(ValidationError("error.required"))
        )))
    }

    "throw error when mandatory timeAtAddress field is missing" in {
      PreviousHomeAddress.formRule.validate(ValidUKForm - "timeAtAddress") must be(
        Failure(Seq(
          (Path \ "timeAtAddress") -> Seq(ValidationError("error.required.timeAtAddress"))
        )))
    }

    "throw error when mandatory postcode field is missing for UK address" in {
      PreviousHomeAddress.formRule.validate(ValidUKForm - "postCode") must be(
        Failure(Seq(
          (Path \ "postCode") -> Seq(ValidationError("error.required"))
        )))
    }

    "throw error when mandatory country field is missing for non UK address" in {
      PreviousHomeAddress.formRule.validate(ValidNonUKForm - "country") must be(
        Failure(Seq(
          (Path \ "country") -> Seq(ValidationError("error.required"))
        )))
    }

    "throw error when there is an invalid data" in {
      val model =  ValidNonUKForm ++ Map("isUK" -> Seq("HGHHHH"))
      PreviousHomeAddress.formRule.validate(model) must be(
        Failure(Seq(
          (Path \ "isUK") -> Seq(ValidationError("error.invalid", "Boolean"))
        )))
    }

    "throw error when an invalid country code is given" in {
      val model =  ValidNonUKForm ++ Map("country" -> Seq("HGHHHH"))
      PreviousHomeAddress.formRule.validate(model) must be(
        Failure(Seq(
          (Path \ "country") -> Seq(ValidationError("error.invalid.country"))
        )))
    }

  }

  "JSON" must {

    val DefaultUKJson = Json.obj(
      "previousAddressLine1" -> DefaultAddressLine1,
      "previousAddressLine2" -> DefaultAddressLine2,
      "previousAddressPostCode" -> DefaultPostcode,
      "previousTimeAtAddress" -> "01"
    )

    val DefaultNonUKJson = Json.obj(
      "previousAddressLine1" -> DefaultAddressLine1,
      "previousAddressLine2" -> DefaultAddressLine2,
      "previousAddressCountry" -> "ES",
      "previousTimeAtAddress" -> "01"
    )

    "Round trip a UK Address correctly" in {
      PreviousHomeAddress.jsonReads.reads(
        PreviousHomeAddress.jsonWrites.writes(ValidUKObject)
      ) must be (JsSuccess(ValidUKObject))
    }

    "Round trip a Non UK Address correctly" in {
      PreviousHomeAddress.jsonReads.reads(
        PreviousHomeAddress.jsonWrites.writes(ValidNonUKObject)
      ) must be (JsSuccess(ValidNonUKObject))
    }

    "Serialise UK address as expected" in {
      Json.toJson(ValidUKObject) must be(DefaultUKJson)
    }

    "Serialise non-UK address as expected" in {
      Json.toJson(ValidNonUKObject) must be(DefaultNonUKJson)
    }

    "Deserialise UK address as expected" in {
      DefaultUKJson.as[PreviousHomeAddress] must be(ValidUKObject)
    }

    "Deserialise non-UK address as expected" in {
      DefaultNonUKJson.as[PreviousHomeAddress] must be(ValidNonUKObject)
    }

  }
}
