/*
 * Copyright 2024 HM Revenue & Customs
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

import models.Country
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}

class WhoIsYourAccountantSpec extends AnyWordSpec with Matchers {

  val testName        = "Default Name"
  val testTradingName = Some("Default Trading Name")

  val testAddressLine1 = "Default Line 1"
  val testAddressLine2 = Some("Default Line 2")
  val testAddressLine3 = Some("Default Line 3")
  val testAddressLine4 = Some("Default Line 4")
  val testPostcode     = "AA1 1AA"
  val testCountry      = Country("France", "FR")

  val testUKAddress =
    UkAccountantsAddress(testAddressLine1, testAddressLine2, testAddressLine3, testAddressLine4, testPostcode)

  val testNonUkAddress =
    NonUkAccountantsAddress(testAddressLine1, testAddressLine2, testAddressLine3, testAddressLine4, testCountry)

  val testWhoIsYourAccountantUk = WhoIsYourAccountant(
    Some(WhoIsYourAccountantName(testName, testTradingName)),
    Some(WhoIsYourAccountantIsUk(true)),
    Some(testUKAddress)
  )

  val testWhoIsYourAccountantNonUk = WhoIsYourAccountant(
    Some(WhoIsYourAccountantName(testName, testTradingName)),
    Some(WhoIsYourAccountantIsUk(false)),
    Some(testNonUkAddress)
  )

  val testUKAccountantJson = Json.obj(
    "accountantsName"            -> testName,
    "accountantsTradingName"     -> testTradingName,
    "isUK"                       -> true,
    "accountantsAddressLine1"    -> testAddressLine1,
    "accountantsAddressLine2"    -> testAddressLine2,
    "accountantsAddressLine3"    -> testAddressLine3,
    "accountantsAddressLine4"    -> testAddressLine4,
    "accountantsAddressPostCode" -> testPostcode
  )

  val testNonUKAccountantJson = Json.obj(
    "accountantsName"           -> testName,
    "accountantsTradingName"    -> testTradingName,
    "isUK"                      -> false,
    "accountantsAddressLine1"   -> testAddressLine1,
    "accountantsAddressLine2"   -> testAddressLine2,
    "accountantsAddressLine3"   -> testAddressLine3,
    "accountantsAddressLine4"   -> testAddressLine4,
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
  }
}
