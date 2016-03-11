package models.businessactivities

import org.scalatest.{MustMatchers, Matchers, WordSpec}
import play.api.libs.json.{JsPath, Json, JsSuccess}

class WhoIsYourAccountantSpec extends WordSpec with Matchers {

  val DefaultName = "Default Name"
  val DefaultTradingName = Some("Default Trading Name")
  val DefaultDealsWithTaxYes = AccountantDoesAlsoDealWithTax("Accountant reference")

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
    DefaultDealsWithTaxYes)

  "WhoIsYourAccountant" must {


    "successfully complete a round robin json conversion" in {
      WhoIsYourAccountant.formats.reads(
        WhoIsYourAccountant.formats.writes(DefaultWhoIsYourAccountant)
      ) shouldBe JsSuccess(DefaultWhoIsYourAccountant)
    }
  }

  "DoesAccountantalsoDealWithTax" must {
    "Serialise yes to json correctly" in {
        val expected = Json.obj(
          "doesAccountantAlsoDealWithTax" -> true,
          "accountantsReference" -> "Accountant reference"
        )
        DoesAccountantAlsoDealWithTax.jsonWrites.writes(DefaultDealsWithTaxYes) should be (expected)
    }

    "Serialise no to json correctly" in {
      val expected = Json.obj(
        "doesAccountantAlsoDealWithTax" -> false
      )
      DoesAccountantAlsoDealWithTax.jsonWrites.writes(AccountantDoesNotAlsoDealWithTax) should be (expected)
    }

    "Deserialise yes from json Correctly" in {
      val json = Json.obj(
        "doesAccountantAlsoDealWithTax" -> true,
        "accountantsReference" -> "Accountant reference"
      )
      DoesAccountantAlsoDealWithTax.jsonReads.reads(json) should be
        (JsSuccess(DefaultDealsWithTaxYes,
         JsPath \ "doesAccountantAlsoDealWithTax" \ "accountantsReference"))
    }

    "Deserialise no from json Correctly" in {
      val json = Json.obj(
        "doesAccountantAlsoDealWithTax" -> false
      )
      DoesAccountantAlsoDealWithTax.jsonReads.reads(json) should be (
        JsSuccess(AccountantDoesNotAlsoDealWithTax, JsPath \"doesAccountantAlsoDealWithTax"))
    }

    "Round trip yes" in {
      DoesAccountantAlsoDealWithTax.jsonReads.reads (
        DoesAccountantAlsoDealWithTax.jsonWrites.writes(DefaultDealsWithTaxYes)
      ) should be (
          JsSuccess(DefaultDealsWithTaxYes,
            JsPath \ "doesAccountantAlsoDealWithTax" \ "accountantsReference"))
    }

    "Round trip no" in {
      DoesAccountantAlsoDealWithTax.jsonReads.reads (
        DoesAccountantAlsoDealWithTax.jsonWrites.writes(AccountantDoesNotAlsoDealWithTax)
      ) should be (JsSuccess(AccountantDoesNotAlsoDealWithTax, JsPath \ "doesAccountantAlsoDealWithTax"))
    }
  }

}
