package models.businessactivities

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.JsSuccess

class WhoIsYourAccountantSpec extends WordSpec with Matchers {

  val DefaultName = "Default Name"
  val DefaultTradingName = Some("Default Trading Name")
  val DefaultDealsWithTax = true

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
    DefaultUKAddress,
    DefaultDealsWithTax)

  "WhoIsYourAccountant" must {

    "successfully complete a round robin json conversion" in {
      WhoIsYourAccountant.formats.reads(
        WhoIsYourAccountant.formats.writes(DefaultWhoIsYourAccountant)
      ) shouldBe JsSuccess(DefaultWhoIsYourAccountant)
    }

  }

}
