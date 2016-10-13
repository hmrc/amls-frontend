package models.businessactivities

import org.scalatest.{MustMatchers, Matchers, WordSpec}
import play.api.data.mapping.Success
import play.api.libs.json.{JsPath, Json, JsSuccess}

class WhoIsYourAccountantSpec extends WordSpec with Matchers {

  val DefaultName = "Default Name"
  val DefaultTradingName = Some("Default Trading Name")

  val DefaultAddressLine1 = "Default Line 1"
  val DefaultAddressLine2 = "Default Line 2"
  val DefaultAddressLine3 = Some("Default Line 3")
  val DefaultAddressLine4 = Some("Default Line 4")
  val DefaultPostcode = "NE1 7YX"
  val DefaultCountry = "Default Country"

  val DefaultUKAddress = UkAccountantsAddress(DefaultAddressLine1,
    DefaultAddressLine2,
    DefaultAddressLine3,
    DefaultAddressLine4,
    DefaultPostcode)

  val DefaultWhoIsYourAccountant = WhoIsYourAccountant(DefaultName,
    DefaultTradingName,
    DefaultUKAddress)

  "WhoIsYourAccountant" must {

    "successfully complete a round trip json conversion" in {
      WhoIsYourAccountant.jsonReads.reads(
        WhoIsYourAccountant.jsonWrites.writes(DefaultWhoIsYourAccountant)
      ) shouldBe JsSuccess(DefaultWhoIsYourAccountant)
    }

    "Successfully complete a round trip to a Url Encoded form" when {
      "The accountant deals with tax matters" in {
        WhoIsYourAccountant.formRule.validate(
          WhoIsYourAccountant.formWrites.writes(WhoIsYourAccountant(DefaultName,
                                                  DefaultTradingName,
                                                  DefaultUKAddress))
        ) should be (Success(WhoIsYourAccountant(DefaultName,
                      DefaultTradingName,
                      DefaultUKAddress)))
      }

      "The accountant does not deal with tax matters" in {
        WhoIsYourAccountant.formRule.validate(
          WhoIsYourAccountant.formWrites.writes(WhoIsYourAccountant(DefaultName,
            DefaultTradingName,
            DefaultUKAddress))
        ) should be (Success(WhoIsYourAccountant(DefaultName,
                            DefaultTradingName,
                            DefaultUKAddress)))
      }
    }
  }

}
