/*
 * Copyright 2017 HM Revenue & Customs
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

import models.{Country, FormTypes}
import org.scalatestplus.play.PlaySpec
import jto.validation.{Valid, Invalid, Path}
import jto.validation.ValidationError
import play.api.libs.json.{JsSuccess, Json}

class AccountantsAddressSpec extends PlaySpec {

  val testAddressLine1 = "Default Line 1"
  val testAddressLine2 = "Default Line 2"
  val testAddressLine3 = Some("Default Line 3")
  val testAddressLine4 = Some("Default Line 4")
  val testPostcode = "AA1 1AA"
  val testCountry = Country("United Kingdom", "GB")

  val NewAddressLine1 = "New Line 1"
  val NewAddressLine2 = "New Line 2"
  val NewAddressLine3 = Some("New Line 3")
  val NewAddressLine4 = Some("New Line 4")
  val NewPostcode = "AA1 1AA"

  val testUKAddress = UkAccountantsAddress(testAddressLine1,
    testAddressLine2,
    testAddressLine3,
    testAddressLine4,
    testPostcode)

  val testNonUKAddress = NonUkAccountantsAddress(testAddressLine1,
    testAddressLine2,
    testAddressLine3,
    testAddressLine4,
    testCountry)

  val testUKJson = Json.obj(
    "accountantsAddressLine1" -> testAddressLine1,
    "accountantsAddressLine2" -> testAddressLine2,
    "accountantsAddressLine3" -> testAddressLine3,
    "accountantsAddressLine4" -> testAddressLine4,
    "accountantsAddressPostCode" -> testPostcode
  )

  val testNonUKJson = Json.obj(
    "accountantsAddressLine1" -> testAddressLine1,
    "accountantsAddressLine2" -> testAddressLine2,
    "accountantsAddressLine3" -> testAddressLine3,
    "accountantsAddressLine4" -> testAddressLine4,
    "accountantsAddressCountry" -> testCountry.code
  )

  val testUKModel = Map(
    "isUK" -> Seq("true"),
    "addressLine1" -> Seq(testAddressLine1),
    "addressLine2" -> Seq(testAddressLine2),
    "addressLine3" -> Seq("Default Line 3"),
    "addressLine4" -> Seq("Default Line 4"),
    "postCode" -> Seq(testPostcode)
  )

  val testNonUKModel = Map(
    "isUK" -> Seq("false"),
    "addressLine1" -> Seq(testAddressLine1),
    "addressLine2" -> Seq(testAddressLine2),
    "addressLine3" -> Seq("Default Line 3"),
    "addressLine4" -> Seq("Default Line 4"),
    "country" -> Seq(testCountry.code)
  )


  "AccountantsAddress" must {

    "validate toLines for UK address" in {
      testUKAddress.toLines must be (Seq("Default Line 1",
        "Default Line 2",
        "Default Line 3",
        "Default Line 4",
        "AA1 1AA"))
    }

    "validate toLines for Non UK address" in {
      testNonUKAddress.toLines must be (Seq("Default Line 1",
        "Default Line 2",
        "Default Line 3",
        "Default Line 4",
        "United Kingdom"))
    }

    "Form validation" must {
      "pass validation" when {
        "given valid Uk address data" in {
          AccountantsAddress.formRule.validate(testUKModel) must be(Valid(testUKAddress))
        }

        "given valid Non-Uk address data" in {
          AccountantsAddress.formRule.validate(testNonUKModel) must be (Valid(testNonUKAddress))
        }
      }

      "fail validation" when {
        "mandatory fields are missing" in {
          AccountantsAddress.formRule.validate(Map.empty) must be(
            Invalid(Seq(
              (Path \ "isUK") -> Seq(ValidationError("error.required.uk.or.overseas"))
            )))
        }

        "isUK is given invalid data" in {
          val model = testNonUKModel ++ Map(
            "isUK" -> Seq("HGHHHH"))
          AccountantsAddress.formRule.validate(model) must be(
            Invalid(Seq(
              (Path \ "isUK") -> Seq(
                ValidationError("error.required.uk.or.overseas")
              )
            )))
        }

        "country is given invalid data" in {
          val model = testNonUKModel ++ Map(
            "country" -> Seq("HGHHHH"))
          AccountantsAddress.formRule.validate(model) must be(
            Invalid(Seq(
              (Path \ "country") -> Seq(
                ValidationError("error.invalid.country")
              )
            )))
        }
      }
    }

    "JSON validation" must {

      "Round trip a UK Address correctly" in {
        AccountantsAddress.jsonReads.reads(
          AccountantsAddress.jsonWrites.writes(testUKAddress)
        ) must be (JsSuccess(testUKAddress))
      }

      "Round trip a Non UK Address correctly" in {
        AccountantsAddress.jsonReads.reads(
          AccountantsAddress.jsonWrites.writes(testNonUKAddress)
        ) must be (JsSuccess(testNonUKAddress))
      }

      "Serialise UK address as expected" in {
        Json.toJson(testUKAddress) must be(testUKJson)
      }

      "Serialise non-UK address as expected" in {
        Json.toJson(testNonUKAddress) must be(testNonUKJson)
      }

      "Deserialise UK address as expected" in {
        testUKJson.as[AccountantsAddress] must be(testUKAddress)
      }

      "Deserialise non-UK address as expected" in {
        testNonUKJson.as[AccountantsAddress] must be(testNonUKAddress)
      }
    }
  }
}
