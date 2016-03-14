package models.businessactivities

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class AccountantsAddressSpec extends PlaySpec {

  val DefaultAddressLine1 = "Default Line 1"
  val DefaultAddressLine2 = "Default Line 2"
  val DefaultAddressLine3 = Some("Default Line 3")
  val DefaultAddressLine4 = Some("Default Line 4")
  val DefaultPostcode = "NE1 7YX"
  val DefaultCountry = "Default Country"

  val NewAddressLine1 = "New Line 1"
  val NewAddressLine2 = "New Line 2"
  val NewAddressLine3 = Some("New Line 3")
  val NewAddressLine4 = Some("New Line 4")
  val NewPostcode = "CR6 5HG"
  val NewCountry = "New Country"

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
    "accountantsAddressCountry" -> DefaultCountry
  )

  "AccountantsAddress" must {

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
