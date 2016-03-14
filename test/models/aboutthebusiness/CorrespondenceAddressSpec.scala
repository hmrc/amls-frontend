package models.aboutthebusiness

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class CorrespondenceAddressSpec extends PlaySpec {

  val DefaultYourName     = "Default Your Name"
  val DefaultBusinessName = "Default Business Name"
  val DefaultAddressLine1 = "Default Line 1"
  val DefaultAddressLine2 = "Default Line 2"
  val DefaultAddressLine3 = Some("Default Line 3")
  val DefaultAddressLine4 = Some("Default Line 4")
  val DefaultPostcode = "NE1 7YX"
  val DefaultCountry = "Default Country"

  val NewYourName     = "New Your Name"
  val NewBusinessName = "New Business Name"
  val NewAddressLine1 = "New Line 1"
  val NewAddressLine2 = "New Line 2"
  val NewAddressLine3 = Some("New Line 3")
  val NewAddressLine4 = Some("New Line 4")
  val NewPostcode = "CR6 5HG"
  val NewCountry = "New Country"

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
