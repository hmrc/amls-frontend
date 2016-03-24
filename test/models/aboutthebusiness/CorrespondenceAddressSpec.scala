package models.aboutthebusiness

import models.FormTypes
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsSuccess, Json}

class CorrespondenceAddressSpec extends PlaySpec {

  val DefaultYourName     = "Default Your Name"
  val DefaultBusinessName = "Default Business Name"
  val DefaultAddressLine1 = "Default Line 1"
  val DefaultAddressLine2 = "Default Line 2"
  val DefaultAddressLine3 = Some("Default Line 3")
  val DefaultAddressLine4 = Some("Default Line 4")
  val DefaultPostcode = "NE1 7YX"
  val DefaultCountry = "MN"

  val NewYourName     = "New Your Name"
  val NewBusinessName = "New Business Name"
  val NewAddressLine1 = "New Line 1"
  val NewAddressLine2 = "New Line 2"
  val NewAddressLine3 = Some("New Line 3")
  val NewAddressLine4 = Some("New Line 4")
  val NewPostcode = "CR6 5HG"
  val NewCountry = "AB"

  val DefaultUKAddress = UKCorrespondenceAddress(DefaultYourName,
    DefaultBusinessName,
    DefaultAddressLine1,
    DefaultAddressLine2,
    DefaultAddressLine3,
    DefaultAddressLine4,
    DefaultPostcode)

  val DefaultNonUKAddress = NonUKCorrespondenceAddress(DefaultYourName,
    DefaultBusinessName,
    DefaultAddressLine1,
    DefaultAddressLine2,
    DefaultAddressLine3,
    DefaultAddressLine4,
    DefaultCountry)

  val DefaultUKModel = Map(
    "isUK" -> Seq("true"),
    "yourName" -> Seq(DefaultYourName),
    "businessName" -> Seq(DefaultBusinessName),
    "addressLine1" -> Seq(DefaultAddressLine1),
    "addressLine2" -> Seq(DefaultAddressLine2),
    "addressLine3" -> Seq("Default Line 3"),
    "addressLine4" -> Seq("Default Line 4"),
    "postCode" -> Seq(DefaultPostcode)
  )

  val DefaultNonUKModel = Map(
    "isUK" -> Seq("false"),
    "yourName" -> Seq(DefaultYourName),
    "businessName" -> Seq(DefaultBusinessName),
    "addressLineNonUK1" -> Seq(DefaultAddressLine1),
    "addressLineNonUK2" -> Seq(DefaultAddressLine2),
    "addressLineNonUK3" -> Seq("Default Line 3"),
    "addressLineNonUK4" -> Seq("Default Line 4"),
    "country" -> Seq(DefaultCountry)
  )


  val DefaultUKJson = Json.obj(
    "yourName"     -> DefaultYourName,
    "businessName" -> DefaultBusinessName,
    "correspondenceAddressLine1" -> DefaultAddressLine1,
    "correspondenceAddressLine2" -> DefaultAddressLine2,
    "correspondenceAddressLine3" -> DefaultAddressLine3,
    "correspondenceAddressLine4" -> DefaultAddressLine4,
    "correspondencePostCode" -> DefaultPostcode
  )

  val DefaultNonUKJson = Json.obj(
    "yourName"     -> DefaultYourName,
    "businessName" -> DefaultBusinessName,
    "correspondenceAddressLine1" -> DefaultAddressLine1,
    "correspondenceAddressLine2" -> DefaultAddressLine2,
    "correspondenceAddressLine3" -> DefaultAddressLine3,
    "correspondenceAddressLine4" -> DefaultAddressLine4,
    "correspondenceCountry" -> DefaultCountry
  )

  "CorrespondenceAddress" must {

    "validate toLines for UK address" in {
      DefaultUKAddress.toLines must be (Seq("Default Your Name",
                                            "Default Business Name",
                                            "Default Line 1",
                                            "Default Line 2",
                                            "Default Line 3",
                                            "Default Line 4",
                                            "NE1 7YX"))

    }

    "validate toLines for Non UK address" in {
      DefaultNonUKAddress.toLines must be (Seq("Default Your Name",
        "Default Business Name",
        "Default Line 1",
        "Default Line 2",
        "Default Line 3",
        "Default Line 4",
        "MN"))

    }

    "Form validation" must {
      "Read UK Address" in {
        CorrespondenceAddress.formRule.validate(DefaultUKModel) must be (Success(DefaultUKAddress))
      }

      "throw error when mandatory fields are missing" in {
        CorrespondenceAddress.formRule.validate(Map.empty) must be
          (Failure(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.atb.uk.or.overseas"))
          )))
      }


      "throw error when there is an invalid data" in {
        val model =  DefaultNonUKModel ++ Map("isUK" -> Seq("HGHHHH"))
        CorrespondenceAddress.formRule.validate(model) must be(
          Failure(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.invalid", "Boolean"))
          )))
      }

      "throw error when length of country exceeds max length" in {
       val model =  DefaultNonUKModel ++ Map("country" -> Seq("HGHHHH"))
        CorrespondenceAddress.formRule.validate(model) must be(
          Failure(Seq(
            (Path \ "country") -> Seq(ValidationError("error.invalid.country", FormTypes.countryRegex))
          )))
      }

      "fail to validation for not filling mandatory field" in {
        val data = Map(
          "isUK" -> Seq("true"),
          "yourName"     -> Seq(DefaultYourName),
          "businessName" -> Seq(DefaultBusinessName),
          "addressLine1" -> Seq(""),
          "addressLine2" -> Seq(""),
          "postCode" -> Seq("")
        )

        CorrespondenceAddress.formRule.validate(data) must
          be(Failure(Seq(
            (Path \ "addressLine1") -> Seq(ValidationError("error.required.address.line1")),
            (Path \ "addressLine2") -> Seq(ValidationError("error.required.address.line2")),
            (Path \ "postCode") -> Seq(ValidationError("error.required.postcode"))
          )))
      }

      "fail to validation for not filling non UK mandatory field" in {
        val data = Map(
          "isUK" -> Seq("false"),
          "yourName"     -> Seq(DefaultYourName),
          "businessName" -> Seq(DefaultBusinessName),
          "addressLineNonUK1" -> Seq(""),
          "addressLineNonUK2" -> Seq(""),
          "country" -> Seq("")
        )

        CorrespondenceAddress.formRule.validate(data) must
          be(Failure(Seq(
            (Path \ "addressLineNonUK1") -> Seq(ValidationError("error.required.address.line1")),
            (Path \ "addressLineNonUK2") -> Seq(ValidationError("error.required.address.line2")),
            (Path \ "country") -> Seq(ValidationError("error.required.country"))
          )))
      }


      "Read Non UK Address" in {
        CorrespondenceAddress.formRule.validate(DefaultNonUKModel) must be (Success(DefaultNonUKAddress))
      }

      "write correct UK Address" in {
        CorrespondenceAddress.formWrites.writes(DefaultUKAddress) must be (DefaultUKModel)
      }

      "write correct Non UK Address" in {
        CorrespondenceAddress.formWrites.writes(DefaultNonUKAddress) must be (DefaultNonUKModel)
      }
    }

    "JSON validation" must {

      "Round trip a UK Address correctly through serialisation" in {
        CorrespondenceAddress.jsonReads.reads(
          CorrespondenceAddress.jsonWrites.writes(DefaultUKAddress)
        ) must be (JsSuccess(DefaultUKAddress))
      }

      "Round trip a Non UK Address correctly through serialisation" in {
        CorrespondenceAddress.jsonReads.reads(
          CorrespondenceAddress.jsonWrites.writes(DefaultNonUKAddress)
        ) must be (JsSuccess(DefaultNonUKAddress))
      }

      "Serialise UK address as expected" in {
        Json.toJson(DefaultUKAddress) must be(DefaultUKJson)
      }

      "Serialise non-UK address as expected" in {
        Json.toJson(DefaultNonUKAddress) must be(DefaultNonUKJson)
      }

      "Deserialise UK address as expected" in {
        DefaultUKJson.as[CorrespondenceAddress] must be(DefaultUKAddress)
      }

      "Deserialise non-UK address as expected" in {
        DefaultNonUKJson.as[CorrespondenceAddress] must be(DefaultNonUKAddress)
      }

    }

  }

}
