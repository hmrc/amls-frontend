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
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class AccountantsAddressSpec extends PlaySpec {

  val testAddressLine1 = "Default Line 1"
  val testAddressLine2 = Some("Default Line 2")
  val testAddressLine3 = Some("Default Line 3")
  val testAddressLine4 = Some("Default Line 4")
  val testPostcode     = "AA1 1AA"
  val testCountry      = Country("United Kingdom", "GB")

  val NewAddressLine1 = "New Line 1"
  val NewAddressLine2 = Some("New Line 2")
  val NewAddressLine3 = Some("New Line 3")
  val NewAddressLine4 = Some("New Line 4")
  val NewPostcode     = "AA1 1AA"

  val testUKAddress =
    UkAccountantsAddress(testAddressLine1, testAddressLine2, testAddressLine3, testAddressLine4, testPostcode)

  val testNonUKAddress =
    NonUkAccountantsAddress(testAddressLine1, testAddressLine2, testAddressLine3, testAddressLine4, testCountry)

  val testUKJson = Json.obj(
    "accountantsAddressLine1"    -> testAddressLine1,
    "accountantsAddressLine2"    -> testAddressLine2,
    "accountantsAddressLine3"    -> testAddressLine3,
    "accountantsAddressLine4"    -> testAddressLine4,
    "accountantsAddressPostCode" -> testPostcode
  )

  val testNonUKJson = Json.obj(
    "accountantsAddressLine1"   -> testAddressLine1,
    "accountantsAddressLine2"   -> testAddressLine2,
    "accountantsAddressLine3"   -> testAddressLine3,
    "accountantsAddressLine4"   -> testAddressLine4,
    "accountantsAddressCountry" -> testCountry.code
  )

  val testUKModel = Map(
    "isUK"         -> Seq("true"),
    "addressLine1" -> Seq(testAddressLine1),
    "addressLine2" -> Seq("Default Line 2"),
    "addressLine3" -> Seq("Default Line 3"),
    "addressLine4" -> Seq("Default Line 4"),
    "postCode"     -> Seq(testPostcode)
  )

  val testNonUKModel = Map(
    "isUK"              -> Seq("false"),
    "addressLineNonUK1" -> Seq(testAddressLine1),
    "addressLineNonUK2" -> Seq("Default Line 2"),
    "addressLineNonUK3" -> Seq("Default Line 3"),
    "addressLineNonUK4" -> Seq("Default Line 4"),
    "country"           -> Seq(testCountry.code)
  )

  "AccountantsAddress" must {

    "validate toLines for UK address" in {
      testUKAddress.toLines must be(
        Seq("Default Line 1", "Default Line 2", "Default Line 3", "Default Line 4", "AA1 1AA")
      )
    }

    "validate toLines for Non UK address" in {
      testNonUKAddress.toLines must be(
        Seq("Default Line 1", "Default Line 2", "Default Line 3", "Default Line 4", "United Kingdom")
      )
    }

    "JSON validation" must {

      "Round trip a UK Address correctly" in {
        AccountantsAddress.jsonReads.reads(
          AccountantsAddress.jsonWrites.writes(testUKAddress)
        ) must be(JsSuccess(testUKAddress))
      }

      "Round trip a Non UK Address correctly" in {
        AccountantsAddress.jsonReads.reads(
          AccountantsAddress.jsonWrites.writes(testNonUKAddress)
        ) must be(JsSuccess(testNonUKAddress))
      }

      "Serialise UK address as expected" in {
        Json.toJson(testUKAddress.asInstanceOf[AccountantsAddress]) must be(testUKJson)
      }

      "Serialise non-UK address as expected" in {
        Json.toJson(testNonUKAddress.asInstanceOf[AccountantsAddress]) must be(testNonUKJson)
      }

      "Deserialise UK address as expected" in {
        testUKJson.as[AccountantsAddress] must be(testUKAddress)
      }

      "Deserialise non-UK address as expected" in {
        testNonUKJson.as[AccountantsAddress] must be(testNonUKAddress)
      }
    }

    "isComplete" must {

      val country = Country("Germany", "DE")

      "return true" when {

        "line 1 and postcode are non-empty for UK address" in {

          assert(UkAccountantsAddress(testAddressLine1, None, None, None, testPostcode).isComplete)
          assert(
            UkAccountantsAddress(
              testAddressLine1,
              testAddressLine2,
              testAddressLine3,
              testAddressLine4,
              testPostcode
            ).isComplete
          )
        }

        "line 1, country code and country name are non-empty for non-UK address" in {

          assert(NonUkAccountantsAddress(testAddressLine1, None, None, None, country).isComplete)
          assert(
            NonUkAccountantsAddress(
              testAddressLine1,
              testAddressLine2,
              testAddressLine3,
              testAddressLine4,
              country
            ).isComplete
          )
        }
      }

      "return false" when {

        "line 1 is empty" when {

          "address is UK" in {

            assert(
              !UkAccountantsAddress("", testAddressLine2, testAddressLine3, testAddressLine4, testPostcode).isComplete
            )
          }

          "address is non-UK" in {

            assert(
              !NonUkAccountantsAddress("", testAddressLine2, testAddressLine3, testAddressLine4, country).isComplete
            )
          }
        }

        "postcode is empty for UK address" in {

          assert(
            !UkAccountantsAddress(testAddressLine1, testAddressLine2, testAddressLine3, testAddressLine4, "").isComplete
          )
        }

        "country code is empty for non-UK address" in {

          assert(
            !NonUkAccountantsAddress(
              testAddressLine1,
              testAddressLine2,
              testAddressLine3,
              testAddressLine4,
              Country("Germany", "")
            ).isComplete
          )
        }

        "country name is empty for non-UK address" in {

          assert(
            !NonUkAccountantsAddress(
              testAddressLine1,
              testAddressLine2,
              testAddressLine3,
              testAddressLine4,
              Country("", "DE")
            ).isComplete
          )
        }
      }
    }
  }
}
