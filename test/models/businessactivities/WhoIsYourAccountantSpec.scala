/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  val testCountry = Country("United States of America", "US")

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

  val testWhoIsYourAccountantUk = WhoIsYourAccountant(
    Some(WhoIsYourAccountantName(testName, testTradingName)),
    Some(WhoIsYourAccountantIsUk(true)),
    Some(testUKAddress))

  val testWhoIsYourAccountantNonUk = WhoIsYourAccountant(
    Some(WhoIsYourAccountantName(testName, testTradingName)),
    Some(WhoIsYourAccountantIsUk(false)),
    Some(testNonUkAddress))

  val testUKAccountantJson = Json.obj(
    "accountantsName" -> testName,
    "accountantsTradingName" -> testTradingName,
    "isUK" -> true,
    "accountantsAddressLine1" -> testAddressLine1,
    "accountantsAddressLine2" -> testAddressLine2,
    "accountantsAddressLine3" -> testAddressLine3,
    "accountantsAddressLine4" -> testAddressLine4,
    "accountantsAddressPostCode" -> testPostcode
  )

  val testNonUKAccountantJson = Json.obj(
    "accountantsName" -> testName,
    "accountantsTradingName" -> testTradingName,
    "isUK" -> false,
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
        WhoIsYourAccountantName.formRule.validate(
          WhoIsYourAccountantName.formWrites.writes(testWhoIsYourAccountantUk.names.get)
        ) should be(Valid(testWhoIsYourAccountantUk.names.get))

        WhoIsYourAccountantIsUk.formRule.validate(
          WhoIsYourAccountantIsUk.formWrites.writes(testWhoIsYourAccountantUk.isUk.get)
        ) should be(Valid(testWhoIsYourAccountantUk.isUk.get))

        AccountantsAddress.ukFormRule.validate(
          AccountantsAddress.formWrites.writes(testWhoIsYourAccountantUk.address.get)
        ) should be(Valid(testWhoIsYourAccountantUk.address.get))
      }

      "given valid data with a Non UK address" in {
        WhoIsYourAccountantName.formRule.validate(
          WhoIsYourAccountantName.formWrites.writes(testWhoIsYourAccountantNonUk.names.get)
        ) should be(Valid(testWhoIsYourAccountantNonUk.names.get))

        WhoIsYourAccountantIsUk.formRule.validate(
          WhoIsYourAccountantIsUk.formWrites.writes(testWhoIsYourAccountantNonUk.isUk.get)
        ) should be(Valid(testWhoIsYourAccountantNonUk.isUk.get))

        AccountantsAddress.nonUkFormRule.validate(
          AccountantsAddress.formWrites.writes(testWhoIsYourAccountantNonUk.address.get)
        ) should be(Valid(testWhoIsYourAccountantNonUk.address.get))
      }
    }

    "fail validation" when {
      "given an empty name" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountantName("", None)

        WhoIsYourAccountantName.formRule.validate(WhoIsYourAccountantName.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be(Invalid(Seq(
          (Path \ "name") -> Seq(ValidationError("error.required.ba.advisor.name"))
        )))
      }

      "given a name with too many characters" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountantName("a" * 141, None)

        WhoIsYourAccountantName.formRule.validate(WhoIsYourAccountantName.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be(Invalid(Seq(
          (Path \ "name") -> Seq(ValidationError("error.length.ba.advisor.name"))
        )))
      }

      "given a name with invalid characters" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountantName("a{}", None)

        WhoIsYourAccountantName.formRule.validate(WhoIsYourAccountantName.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be(Invalid(Seq(
          (Path \ "name") -> Seq(ValidationError("error.punctuation.ba.advisor.name"))
        )))
      }

      "given a trading name with too many characters" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountantName(testName, Some("a" * 121))

        WhoIsYourAccountantName.formRule.validate(WhoIsYourAccountantName.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be(Invalid(Seq(
          (Path \ "tradingName") -> Seq(ValidationError("error.length.ba.advisor.tradingname"))
        )))
      }

      "given a trading name with invalid characters" in {
        val DefaultWhoIsYourAccountant = WhoIsYourAccountantName(testName, Some("a{}"))

        WhoIsYourAccountantName.formRule.validate(WhoIsYourAccountantName.formWrites.writes(DefaultWhoIsYourAccountant)
        ) should be(Invalid(Seq(
          (Path \ "tradingName") -> Seq(ValidationError("error.punctuation.ba.advisor.tradingname"))
        )))
      }
    }
  }
}
