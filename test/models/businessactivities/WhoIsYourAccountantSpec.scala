package models.businessactivities

import jto.validation.{Invalid, Path, Valid, ValidationError}
import models.Country
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsSuccess, Json}

class WhoIsYourAccountantSpec extends WordSpec with Matchers {

  val testName = "Default Name"
  val testTradingName = Some("Default Trading Name")

  val testAddressLine1 = "Default Line 1"
  val testAddressLine2 = "Default Line 2"
  val testAddressLine3 = Some("Default Line 3")
  val testAddressLine4 = Some("Default Line 4")
  val testPostcode = "AA1 1AA"
  val testCountry = Country("United States", "US")

  val testUKAddress = UkAccountantsAddress(testAddressLine1,
    testAddressLine2,
    testAddressLine3,
    testAddressLine4,
    testPostcode)

  val testNonUkAddress = NonUkAccountantsAddress(testAddressLine1,
    testAddressLine2,
    testAddressLine3,
    testAddressLine4,
    testCountry)

  val testWhoIsYourAccountantUk = WhoIsYourAccountant(testName,
    testTradingName,
    testUKAddress)

  val testWhoIsYourAccountantNonUk = WhoIsYourAccountant(testName,
    testTradingName,
    testNonUkAddress)

  val testUKAccountantJson = Json.obj(
    "accountantsName" -> testName,
    "accountantsTradingName" -> testTradingName,
    "accountantsAddressLine1" -> testAddressLine1,
    "accountantsAddressLine2" -> testAddressLine2,
    "accountantsAddressLine3" -> testAddressLine3,
    "accountantsAddressLine4" -> testAddressLine4,
    "accountantsAddressPostCode" -> testPostcode
  )

  val testNonUKAccountantJson = Json.obj(
    "accountantsName" -> testName,
    "accountantsTradingName" -> testTradingName,
    "accountantsAddressLine1" -> testAddressLine1,
    "accountantsAddressLine2" -> testAddressLine2,
    "accountantsAddressLine3" -> testAddressLine3,
    "accountantsAddressLine4" -> testAddressLine4,
    "accountantsAddressCountry" -> testCountry.code
  )

  "WhoIsYourAccountant" must {

    "successfully complete a round trip json conversion" in {
      WhoIsYourAccountant.jsonReads.reads(
        WhoIsYourAccountant.jsonWrites.writes(testWhoIsYourAccountantUk)
      ) shouldBe JsSuccess(testWhoIsYourAccountantUk)
    }

    "Serialise UK address as expected" in {
      Json.toJson(testWhoIsYourAccountantUk) should be(testUKAccountantJson)
    }

    "Serialise non-UK address as expected" in {
      Json.toJson(testWhoIsYourAccountantNonUk) should be(testNonUKAccountantJson)
    }

    "Deserialise UK address as expected" in {
      testUKAccountantJson.as[WhoIsYourAccountant] should be(testWhoIsYourAccountantUk)
    }

    "Deserialise non-UK address as expected" in {
      testNonUKAccountantJson.as[WhoIsYourAccountant] should be(testWhoIsYourAccountantNonUk)
    }

    "pass validation" when {
      "given valid data with a UK address" in {
        WhoIsYourAccountant.formRule.validate(
          WhoIsYourAccountant.formWrites.writes(testWhoIsYourAccountantUk)
        ) should be(Valid(testWhoIsYourAccountantUk))
      }

      "given valid data with a Non UK address" in {
        WhoIsYourAccountant.formRule.validate(
          WhoIsYourAccountant.formWrites.writes(testWhoIsYourAccountantNonUk)
        ) should be(Valid(testWhoIsYourAccountantNonUk))
      }
    }

    "fail validation" when {
      "given an empty name" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountant("",
          testTradingName,
          testUKAddress)

        WhoIsYourAccountant.formRule.validate(WhoIsYourAccountant.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be(Invalid(Seq(
          (Path \ "name") -> Seq(ValidationError("error.required.ba.advisor.name"))
        )))
      }

      "given a name with too many characters" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountant("a" * 141,
          testTradingName,
          testUKAddress)

        WhoIsYourAccountant.formRule.validate(WhoIsYourAccountant.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be(Invalid(Seq(
          (Path \ "name") -> Seq(ValidationError("error.invalid.maxlength.140"))
        )))
      }

      "given a name with invalid characters" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountant("sasdasd{}sdfsdf",
          testTradingName,
          testUKAddress)

        WhoIsYourAccountant.formRule.validate(WhoIsYourAccountant.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be(Invalid(Seq(
          (Path \ "name") -> Seq(ValidationError("err.text.validation"))
        )))
      }

      "given a trading name with too many characters" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountant(testName,
          Some("a" * 121),
          testUKAddress
        )

        WhoIsYourAccountant.formRule.validate(WhoIsYourAccountant.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be(Invalid(Seq(
          (Path \ "tradingName") -> Seq(ValidationError("error.invalid.maxlength.120"))
        )))
      }

      "given a trading name with invalid characters" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountant(testName,
          Some("sasdasd{}sdfsdf"),
          testUKAddress)

        WhoIsYourAccountant.formRule.validate(WhoIsYourAccountant.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be(Invalid(Seq(
          (Path \ "tradingName") -> Seq(ValidationError("err.text.validation"))
        )))
      }
    }
  }
}
