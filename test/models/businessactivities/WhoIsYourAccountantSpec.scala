package models.businessactivities

import org.scalatest.{Matchers, MustMatchers, WordSpec}
import play.api.libs.json.{JsPath, JsSuccess, Json}
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError

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
        ) should be (Valid(WhoIsYourAccountant(DefaultName,
                      DefaultTradingName,
                      DefaultUKAddress)))
      }

      "The accountant does not deal with tax matters" in {
        WhoIsYourAccountant.formRule.validate(
          WhoIsYourAccountant.formWrites.writes(WhoIsYourAccountant(DefaultName,
            DefaultTradingName,
            DefaultUKAddress))
        ) should be (Valid(WhoIsYourAccountant(DefaultName,
                            DefaultTradingName,
                            DefaultUKAddress)))
      }

      "fail to validate given an empty name" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountant("",
          DefaultTradingName,
          DefaultUKAddress)

        WhoIsYourAccountant.formRule.validate(WhoIsYourAccountant.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be (Invalid(Seq(
          (Path \ "name") -> Seq(ValidationError("error.required.ba.advisor.name"))
        )))
      }

      "fail to validate given a name with too many characters" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountant("zxzxcz"*50,
          DefaultTradingName,
          DefaultUKAddress)

        WhoIsYourAccountant.formRule.validate(WhoIsYourAccountant.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be (Invalid(Seq(
          (Path \ "name") -> Seq(ValidationError("error.invalid.maxlength.140"))
        )))
      }

      "fail to validate given a name with invalid characters" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountant("sasdasd{}sdfsdf",
          DefaultTradingName,
          DefaultUKAddress)

        WhoIsYourAccountant.formRule.validate(WhoIsYourAccountant.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be (Invalid(Seq(
          (Path \ "name") -> Seq(ValidationError("err.text.validation"))
        )))


      }
    }
  }

}
