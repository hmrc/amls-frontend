/*
 * Copyright 2018 HM Revenue & Customs
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

package models.aboutthebusiness

import cats.data.Validated.{Invalid, Valid}
import models.Country
import org.scalatestplus.play.PlaySpec
import jto.validation.Path
import jto.validation.ValidationError
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.{JsSuccess, Json}

class CorrespondenceAddressSpec extends PlaySpec with MockitoSugar {

  "CorrespondenceAddress" must {

    "validate toLines" when {
      "given a UK address" in {
        DefaultUKAddress.toLines must be(Seq("Default Your Name",
          "Default Business Name",
          "Default Line 1",
          "Default Line 2",
          "Default Line 3",
          "Default Line 4",
          "AA1 1AA"))

      }

      "given a Non UK address" in {
        DefaultNonUKAddress.toLines must be(Seq("Default Your Name",
          "Default Business Name",
          "Default Line 1",
          "Default Line 2",
          "Default Line 3",
          "Default Line 4",
          "United Kingdom"))
      }
    }
  }

    "CorrespondenceAddress Form validation" must {
      "throw error when mandatory fields are missing" in {
        CorrespondenceAddress.formRule.validate(Map.empty) must be
        Invalid(Seq(
          (Path \ "isUK") -> Seq(ValidationError("error.required.uk.or.overseas"))
        ))
      }

      "throw error when there is an invalid data" in {
        val model = DefaultNonUKModel ++ Map("isUK" -> Seq("HGHHHH"))
        CorrespondenceAddress.formRule.validate(model) must be(
          Invalid(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.uk.or.overseas"))
          )))
      }
    }

    "Form validation" when {
      "given a UK address" must {
        "throw errors when mandatory fields are represented by an empty string" in {
          val data = Map(
            "isUK" -> Seq("true"),
            "yourName" -> Seq(""),
            "businessName" -> Seq(""),
            "addressLine1" -> Seq(""),
            "addressLine2" -> Seq(""),
            "postCode" -> Seq("")
          )

          CorrespondenceAddress.formRule.validate(data) must
            be(Invalid(Seq(
              (Path \ "yourName") -> Seq(ValidationError("error.required.yourname")),
              (Path \ "businessName") -> Seq(ValidationError("error.required.name.of.business")),
              (Path \ "addressLine1") -> Seq(ValidationError("error.required.address.line1")),
              (Path \ "addressLine2") -> Seq(ValidationError("error.required.address.line2")),
              (Path \ "postCode") -> Seq(ValidationError("error.invalid.postcode"))
            )))
        }

        "throw errors when number of characters entered into fields exceed max length" in {
          val model = Map(
            "isUK" -> Seq("true"),
            "yourName" -> Seq("a" * 150),
            "businessName" -> Seq("b" * 150),
            "addressLine1" -> Seq("c" * 41),
            "addressLine2" -> Seq("d" * 41),
            "addressLine3" -> Seq("e" * 41),
            "addressLine4" -> Seq("f" * 41),
            "postCode" -> Seq("A" * 50)
          )

          CorrespondenceAddress.formRule.validate(model) must be(
            Invalid(Seq(
              (Path \ "yourName") -> Seq(ValidationError("error.invalid.yourname")),
              (Path \ "businessName") -> Seq(ValidationError("error.invalid.name.of.business")),
              (Path \ "addressLine1") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLine2") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLine3") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLine4") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "postCode") -> Seq(ValidationError("error.invalid.postcode"))
            )))
        }

        "Read UK Address" in {
          CorrespondenceAddress.formRule.validate(DefaultUKModel) must be(Valid(DefaultUKAddress))
        }

        "write correct UK Address" in {
          CorrespondenceAddress.formWrites.writes(DefaultUKAddress) must be(DefaultUKModel)
        }

      }

      "given a Non UK Address" must {
        "throw errors when number of characters entered into fields exceed max length" in {
          val model = Map(
            "isUK" -> Seq("false"),
            "yourName" -> Seq("a" * 150),
            "businessName" -> Seq("b" * 150),
            "addressLineNonUK1" -> Seq("c" * 41),
            "addressLineNonUK2" -> Seq("d" * 41),
            "addressLineNonUK3" -> Seq("e" * 41),
            "addressLineNonUK4" -> Seq("f" * 41),
            "country" -> Seq("A" * 10)
          )

          CorrespondenceAddress.formRule.validate(model) must be(
            Invalid(Seq(
              (Path \ "yourName") -> Seq(ValidationError("error.invalid.yourname")),
              (Path \ "businessName") -> Seq(ValidationError("error.invalid.name.of.business")),
              (Path \ "addressLineNonUK1") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLineNonUK2") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLineNonUK3") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLineNonUK4") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "country") -> Seq(ValidationError("error.invalid.country"))
            )))
        }

        "fail validation for not filling non UK mandatory field represented by empty string" in {
          val data = Map(
            "isUK" -> Seq("false"),
            "yourName" -> Seq(""),
            "businessName" -> Seq(""),
            "addressLineNonUK1" -> Seq(""),
            "addressLineNonUK2" -> Seq(""),
            "country" -> Seq("")
          )

          CorrespondenceAddress.formRule.validate(data) must
            be(Invalid(Seq(
              (Path \ "yourName") -> Seq(ValidationError("error.required.yourname")),
              (Path \ "businessName") -> Seq(ValidationError("error.required.name.of.business")),
              (Path \ "addressLineNonUK1") -> Seq(ValidationError("error.required.address.line1")),
              (Path \ "addressLineNonUK2") -> Seq(ValidationError("error.required.address.line2")),
              (Path \ "country") -> Seq(ValidationError("error.required.country"))
            )))
        }

        "Read Non UK Address" in {
          CorrespondenceAddress.formRule.validate(DefaultNonUKModel) must be(Valid(DefaultNonUKAddress))
        }

        "write correct Non UK Address" in {
          CorrespondenceAddress.formWrites.writes(DefaultNonUKAddress) must be(DefaultNonUKModel)
        }
      }
    }

    "JSON validation" must {

      "Round trip a UK Address correctly through serialisation" in {
        CorrespondenceAddress.jsonReads.reads(
          CorrespondenceAddress.jsonWrites.writes(DefaultUKAddress)
        ) must be(JsSuccess(DefaultUKAddress))
      }

      "Round trip a Non UK Address correctly through serialisation" in {
        CorrespondenceAddress.jsonReads.reads(
          CorrespondenceAddress.jsonWrites.writes(DefaultNonUKAddress)
        ) must be(JsSuccess(DefaultNonUKAddress))
      }

      "Serialise UK address as expected" in {
        Json.toJson(DefaultUKAddress) must be(DefaultUKJson)
      }

      "Serialise non-UK address as expected" in {
        Json.toJson(DefaultNonUKAddress) must be(DefaultNonUKJson)
      }

      "Deserialise UK address as expected" in {
        DefaultUKJson.as[CorrespondenceAddress] must be(DefaultUKAddress)
      }

      "Deserialise non-UK address as expected" in {
        DefaultNonUKJson.as[CorrespondenceAddress] must be(DefaultNonUKAddress)
      }

    }



  val DefaultYourName = "Default Your Name"
  val DefaultBusinessName = "Default Business Name"
  val DefaultAddressLine1 = "Default Line 1"
  val DefaultAddressLine2 = "Default Line 2"
  val DefaultAddressLine3 = Some("Default Line 3")
  val DefaultAddressLine4 = Some("Default Line 4")
  val DefaultPostcode = "AA1 1AA"
  val DefaultCountry = Country("United Kingdom", "GB")

  val NewYourName = "New Your Name"
  val NewBusinessName = "New Business Name"
  val NewAddressLine1 = "New Line 1"
  val NewAddressLine2 = "New Line 2"
  val NewAddressLine3 = Some("New Line 3")
  val NewAddressLine4 = Some("New Line 4")
  val NewPostcode = "AA1 1AA"
  val NewCountry = "AB"

  val DefaultUKAddress = UKCorrespondenceAddress(DefaultYourName,
    DefaultBusinessName,
    DefaultAddressLine1,
    DefaultAddressLine2,
    DefaultAddressLine3,
    DefaultAddressLine4,
    DefaultPostcode)

  val DefaultNonUKAddress = NonUKCorrespondenceAddress(DefaultYourName,
    DefaultBusinessName,
    DefaultAddressLine1,
    DefaultAddressLine2,
    DefaultAddressLine3,
    DefaultAddressLine4,
    DefaultCountry)

  val DefaultUKModel = Map(
    "isUK" -> Seq("true"),
    "yourName" -> Seq(DefaultYourName),
    "businessName" -> Seq(DefaultBusinessName),
    "addressLine1" -> Seq(DefaultAddressLine1),
    "addressLine2" -> Seq(DefaultAddressLine2),
    "addressLine3" -> Seq("Default Line 3"),
    "addressLine4" -> Seq("Default Line 4"),
    "postCode" -> Seq(DefaultPostcode)
  )

  val DefaultNonUKModel = Map(
    "isUK" -> Seq("false"),
    "yourName" -> Seq(DefaultYourName),
    "businessName" -> Seq(DefaultBusinessName),
    "addressLineNonUK1" -> Seq(DefaultAddressLine1),
    "addressLineNonUK2" -> Seq(DefaultAddressLine2),
    "addressLineNonUK3" -> Seq("Default Line 3"),
    "addressLineNonUK4" -> Seq("Default Line 4"),
    "country" -> Seq(DefaultCountry.code)
  )


  val DefaultUKJson = Json.obj(
    "yourName" -> DefaultYourName,
    "businessName" -> DefaultBusinessName,
    "correspondenceAddressLine1" -> DefaultAddressLine1,
    "correspondenceAddressLine2" -> DefaultAddressLine2,
    "correspondenceAddressLine3" -> DefaultAddressLine3,
    "correspondenceAddressLine4" -> DefaultAddressLine4,
    "correspondencePostCode" -> DefaultPostcode
  )

  val DefaultNonUKJson = Json.obj(
    "yourName" -> DefaultYourName,
    "businessName" -> DefaultBusinessName,
    "correspondenceAddressLine1" -> DefaultAddressLine1,
    "correspondenceAddressLine2" -> DefaultAddressLine2,
    "correspondenceAddressLine3" -> DefaultAddressLine3,
    "correspondenceAddressLine4" -> DefaultAddressLine4,
    "correspondenceCountry" -> DefaultCountry
  )

}
