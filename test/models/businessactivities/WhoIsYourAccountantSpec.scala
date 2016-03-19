package models.businessactivities

import org.scalatest.{MustMatchers, Matchers, WordSpec}
import play.api.data.mapping.Success
import play.api.libs.json.{JsPath, Json, JsSuccess}

class WhoIsYourAccountantSpec extends WordSpec with Matchers {

  val DefaultName = "Default Name"
  val DefaultTradingName = Some("Default Trading Name")
  val DefaultDealsWithTaxYes = AccountantDoesAlsoDealWithTax("11Character")

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
                                                  DefaultUKAddress,
                                                  AccountantDoesAlsoDealWithTax("11Character")))
        ) should be (Success(WhoIsYourAccountant(DefaultName,
                      DefaultTradingName,
                      DefaultUKAddress,
                      AccountantDoesAlsoDealWithTax("11Character"))))
      }

      "The accountant does not deal with tax matters" in {
        WhoIsYourAccountant.formRule.validate(
          WhoIsYourAccountant.formWrites.writes(WhoIsYourAccountant(DefaultName,
            DefaultTradingName,
            DefaultUKAddress,
            AccountantDoesNotAlsoDealWithTax))
        ) should be (Success(WhoIsYourAccountant(DefaultName,
                            DefaultTradingName,
                            DefaultUKAddress,
                            AccountantDoesNotAlsoDealWithTax)))
      }
    }
  }

  "DoesAccountantalsoDealWithTax" must {
    "Serialise yes to json correctly" in {
        val expected = Json.obj(
          "doesAccountantAlsoDealWithTax" -> true,
          "accountantsReference" -> "11Character"
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

    "Write to a form correctly" when {
      "the accountant deals with tax matters" in {
        val expected = Map("alsoDealsWithTax" -> Seq("true"), "accountantsReferenceNumber" -> Seq("RefNo"))
        DoesAccountantAlsoDealWithTax.formWrites.writes(AccountantDoesAlsoDealWithTax("RefNo")) should be (expected)
      }

      "the accountant does not deal with tax matters" in {
        val expected = Map("alsoDealsWithTax" -> Seq("false"))
        DoesAccountantAlsoDealWithTax.formWrites.writes(AccountantDoesNotAlsoDealWithTax) should be (expected)
      }
    }

    "Read from a form correctly" when {
      "the accountant deals with tax matters" in {
        val form = Map("alsoDealsWithTax" -> Seq("true"), "accountantsReferenceNumber" -> Seq("01234567891"))
        DoesAccountantAlsoDealWithTax.formRule.validate(form) should be (Success(AccountantDoesAlsoDealWithTax("01234567891")))
      }

      "the accountant does not deal with tax matters" in {
        val form = Map("alsoDealsWithTax" -> Seq("false"))
        DoesAccountantAlsoDealWithTax.formRule.validate(form) should be (Success(AccountantDoesNotAlsoDealWithTax))
      }
    }
  }

}
