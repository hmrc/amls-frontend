/*
 * Copyright 2019 HM Revenue & Customs
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

package models.responsiblepeople

import models.Country
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsSuccess, Json}

class PersonAddressSpec extends PlaySpec {

  val DefaultAddressLine1 = "Default Line 1"
  val DefaultAddressLine2 = "Default Line 2"
  val DefaultAddressLine3 = Some("Default Line 3")
  val DefaultAddressLine4 = Some("Default Line 4")
  val DefaultPostcode = "AA1 1AA"
  val DefaultCountry = Country("Albania", "AL")

  val NewAddressLine1 = "New Line 1"
  val NewAddressLine2 = "New Line 2"
  val NewAddressLine3 = Some("New Line 3")
  val NewAddressLine4 = Some("New Line 4")
  val NewPostcode = "AA1 1AA"
  val NewCountry = "AB"

  val DefaultUKAddress = PersonAddressUK(
    DefaultAddressLine1,
    DefaultAddressLine2,
    DefaultAddressLine3,
    DefaultAddressLine4,
    DefaultPostcode)

  val DefaultNonUKAddress = PersonAddressNonUK(
    DefaultAddressLine1,
    DefaultAddressLine2,
    DefaultAddressLine3,
    DefaultAddressLine4,
    DefaultCountry)

  val DefaultUKModel = Map(
    "isUK" -> Seq("true"),
    "addressLine1" -> Seq(DefaultAddressLine1),
    "addressLine2" -> Seq(DefaultAddressLine2),
    "addressLine3" -> Seq("Default Line 3"),
    "addressLine4" -> Seq("Default Line 4"),
    "postCode" -> Seq(DefaultPostcode)
  )

  val DefaultNonUKModel = Map(
    "isUK" -> Seq("false"),
    "addressLineNonUK1" -> Seq(DefaultAddressLine1),
    "addressLineNonUK2" -> Seq(DefaultAddressLine2),
    "addressLineNonUK3" -> Seq("Default Line 3"),
    "addressLineNonUK4" -> Seq("Default Line 4"),
    "country" -> Seq(DefaultCountry.code)
  )

  val DefaultUKJson = Json.obj(
    "personAddressLine1" -> DefaultAddressLine1,
    "personAddressLine2" -> DefaultAddressLine2,
    "personAddressLine3" -> DefaultAddressLine3,
    "personAddressLine4" -> DefaultAddressLine4,
    "personAddressPostCode" -> DefaultPostcode
  )

  val DefaultNonUKJson = Json.obj(
    "personAddressLine1" -> DefaultAddressLine1,
    "personAddressLine2" -> DefaultAddressLine2,
    "personAddressLine3" -> DefaultAddressLine3,
    "personAddressLine4" -> DefaultAddressLine4,
    "personAddressCountry" -> DefaultCountry
  )

  "personAddress" must {

    "validate toLines for UK address" in {
      DefaultUKAddress.toLines must be(Seq("Default Line 1",
        "Default Line 2",
        "Default Line 3",
        "Default Line 4",
        "AA1 1AA"))

    }

    "validate toLines for Non UK address" in {
      DefaultNonUKAddress.toLines must be(Seq(
        "Default Line 1",
        "Default Line 2",
        "Default Line 3",
        "Default Line 4",
        "Albania"))
    }

    "pass validation" when {
      "Reading UK Address" in {
        PersonAddress.formRule.validate(DefaultUKModel) must be(Valid(DefaultUKAddress))
      }
      "Read Non UK Address" in {
        PersonAddress.formRule.validate(DefaultNonUKModel) must be(Valid(DefaultNonUKAddress))
      }
    }

    "throw error" when {
      "given a non valid non UK address" in {
        val invalidNonUKModel = Map(
          "isUK" -> Seq("false"),
          "addressLineNonUK1" -> Seq(DefaultAddressLine1),
          "addressLineNonUK2" -> Seq(DefaultAddressLine2),
          "addressLineNonUK3" -> Seq("Default Line 3"),
          "addressLineNonUK4" -> Seq("Default Line 4"),
          "country" -> Seq("GB")
        )

        PersonAddress.formRule.validate(invalidNonUKModel) must
          be(Invalid(Seq(
            (Path \ "country") -> Seq(ValidationError("error.required.non.uk.country"))
          )))
      }

      "mandatory fields are missing" when {
        "isUK has not been selected" in {
          PersonAddress.formRule.validate(DefaultUKModel) must be
          Invalid(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.uk.or.overseas"))
          ))
        }
        "address lines are missing" when {
          "UK" in {
            val data = Map(
              "isUK" -> Seq("true"),
              "addressLine1" -> Seq(""),
              "addressLine2" -> Seq(""),
              "postCode" -> Seq("")
            )

            PersonAddress.formRule.validate(data) must
              be(Invalid(Seq(
                (Path \ "addressLine1") -> Seq(ValidationError("error.required.address.line1")),
                (Path \ "addressLine2") -> Seq(ValidationError("error.required.address.line2")),
                (Path \ "postCode") -> Seq(ValidationError("error.invalid.postcode"))
              )))
          }
          "non UK" in {
            val data = Map(
              "isUK" -> Seq("false"),
              "addressLineNonUK1" -> Seq(""),
              "addressLineNonUK2" -> Seq(""),
              "country" -> Seq("")
            )

            PersonAddress.formRule.validate(data) must
              be(Invalid(Seq(
                (Path \ "addressLineNonUK1") -> Seq(ValidationError("error.required.address.line1")),
                (Path \ "addressLineNonUK2") -> Seq(ValidationError("error.required.address.line2")),
                (Path \ "country") -> Seq(ValidationError("error.required.country"))
              )))
          }
        }
      }
      "values given are too long" when {
        "UK" in {
          val data = Map(
            "isUK" -> Seq("true"),
            "addressLine1" -> Seq("abc" * 35),
            "addressLine2" -> Seq("abc" * 35),
            "addressLine3" -> Seq("abc" * 35),
            "addressLine4" -> Seq("abc" * 35),
            "postCode" -> Seq("abc" * 35)
          )
          PersonAddress.formRule.validate(data) must be(
            Invalid(Seq(
              (Path \ "addressLine1") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLine2") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLine3") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLine4") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "postCode") -> Seq(ValidationError("error.invalid.postcode"))
            )))
        }
        "non UK" in {
          val data = Map(
            "isUK" -> Seq("false"),
            "addressLineNonUK1" -> Seq("abc" * 35),
            "addressLineNonUK2" -> Seq("abc" * 35),
            "addressLineNonUK3" -> Seq("abc" * 35),
            "addressLineNonUK4" -> Seq("abc" * 35),
            "country" -> Seq("abc" * 35)
          )
          PersonAddress.formRule.validate(data) must be(
            Invalid(Seq(
              (Path \ "addressLineNonUK1") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLineNonUK2") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLineNonUK3") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "addressLineNonUK4") -> Seq(ValidationError("error.max.length.address.line")),
              (Path \ "country") -> Seq(ValidationError("error.invalid.country"))
            )))
        }
      }
      "there is invalid data" in {
        val model = DefaultNonUKModel ++ Map("isUK" -> Seq("HGHHHH"))
        PersonAddress.formRule.validate(model) must be(
          Invalid(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.uk.or.overseas"))
          )))
      }
    }

    "write correct UK Address" in {
      PersonAddress.formWrites.writes(DefaultUKAddress) must be(DefaultUKModel)
    }

    "write correct Non UK Address" in {
      PersonAddress.formWrites.writes(DefaultNonUKAddress) must be(DefaultNonUKModel)
    }
  }

  "JSON validation" must {

    "Round trip a UK Address correctly through serialisation" in {
      PersonAddress.jsonReads.reads(
        PersonAddress.jsonWrites.writes(DefaultUKAddress)
      ) must be(JsSuccess(DefaultUKAddress))
    }

    "Round trip a Non UK Address correctly through serialisation" in {
      PersonAddress.jsonReads.reads(
        PersonAddress.jsonWrites.writes(DefaultNonUKAddress)
      ) must be(JsSuccess(DefaultNonUKAddress))
    }

    "Serialise UK address as expected" in {
      Json.toJson(DefaultUKAddress) must be(DefaultUKJson)
    }

    "Serialise non-UK address as expected" in {
      Json.toJson(DefaultNonUKAddress) must be(DefaultNonUKJson)
    }

    "Deserialise UK address as expected" in {
      DefaultUKJson.as[PersonAddress] must be(DefaultUKAddress)
    }

    "Deserialise non-UK address as expected" in {
      DefaultNonUKJson.as[PersonAddress] must be(DefaultNonUKAddress)
    }

  }

}
