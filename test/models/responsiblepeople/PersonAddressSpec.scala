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

package models.responsiblepeople

import models.Country
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsSuccess, Json}

class PersonAddressSpec extends PlaySpec {

  val defaultAddressLine1 = "Default Line 1"
  val defaultAddressLine2 = Some("Default Line 2")
  val defaultAddressLine3 = Some("Default Line 3")
  val defaultAddressLine4 = Some("Default Line 4")
  val defaultPostcode = "AA1 1AA"
  val defaultCountry = Country("Albania", "AL")

  val newAddressLine1 = "New Line 1"
  val newAddressLine2 = Some("New Line 2")
  val newAddressLine3 = Some("New Line 3")
  val newAddressLine4 = Some("New Line 4")
  val newPostcode = "AA1 1AA"
  val newCountry = "AB"

  val defaultUKAddress = PersonAddressUK(
    defaultAddressLine1,
    defaultAddressLine2,
    defaultAddressLine3,
    defaultAddressLine4,
    defaultPostcode)

  val defaultNonUKAddress = PersonAddressNonUK(
    defaultAddressLine1,
    defaultAddressLine2,
    defaultAddressLine3,
    defaultAddressLine4,
    defaultCountry)

  val defaultUKModel = Map(
    "isUK" -> Seq("true"),
    "addressLine1" -> Seq(defaultAddressLine1),
    "addressLine2" -> Seq("Default Line 2"),
    "addressLine3" -> Seq("Default Line 3"),
    "addressLine4" -> Seq("Default Line 4"),
    "postCode" -> Seq(defaultPostcode)
  )

  val defaultNonUKModel = Map(
    "isUK" -> Seq("false"),
    "addressLineNonUK1" -> Seq(defaultAddressLine1),
    "addressLineNonUK2" -> Seq("Default Line 2"),
    "addressLineNonUK3" -> Seq("Default Line 3"),
    "addressLineNonUK4" -> Seq("Default Line 4"),
    "country" -> Seq(defaultCountry.code)
  )

  val defaultUKJson = Json.obj(
    "personAddressLine1" -> defaultAddressLine1,
    "personAddressLine2" -> defaultAddressLine2,
    "personAddressLine3" -> defaultAddressLine3,
    "personAddressLine4" -> defaultAddressLine4,
    "personAddressPostCode" -> defaultPostcode
  )

  val defaultNonUKJson = Json.obj(
    "personAddressLine1" -> defaultAddressLine1,
    "personAddressLine2" -> defaultAddressLine2,
    "personAddressLine3" -> defaultAddressLine3,
    "personAddressLine4" -> defaultAddressLine4,
    "personAddressCountry" -> defaultCountry
  )

  "personAddress" must {

    "validate toLines for UK address" in {
      defaultUKAddress.toLines must be(Seq("Default Line 1",
        "Default Line 2",
        "Default Line 3",
        "Default Line 4",
        "AA1 1AA"))

    }

    "validate toLines for Non UK address" in {
      defaultNonUKAddress.toLines must be(Seq(
        "Default Line 1",
        "Default Line 2",
        "Default Line 3",
        "Default Line 4",
        "Albania"))
    }

    "pass validation" when {
      "Reading UK Address" in {
        PersonAddress.formRule.validate(defaultUKModel) must be(Valid(defaultUKAddress))
      }
      "Read Non UK Address" in {
        PersonAddress.formRule.validate(defaultNonUKModel) must be(Valid(defaultNonUKAddress))
      }
    }

    "throw error" when {
      "given a non valid non UK address" in {
        val invalidNonUKModel = Map(
          "isUK" -> Seq("false"),
          "addressLineNonUK1" -> Seq(defaultAddressLine1),
          "addressLineNonUK2" -> Seq("Default Line 2"),
          "addressLineNonUK3" -> Seq("Default Line 3"),
          "addressLineNonUK4" -> Seq("Default Line 4"),
          "country" -> Seq("GB")
        )

        PersonAddress.formRule.validate(invalidNonUKModel) must
          be(Invalid(Seq(
            (Path \ "country") -> Seq(ValidationError("error.required.select.non.uk"))
          )))
      }

      "mandatory fields are missing" when {
        "isUK has not been selected" in {
          PersonAddress.formRule.validate(defaultUKModel) must be
          Invalid(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.uk.or.overseas"))
          ))
        }
        "address lines are missing" when {
          "UK" in {
            val data = Map(
              "isUK" -> Seq("true"),
              "addressLine1" -> Seq(""),
              "postCode" -> Seq("")
            )

            PersonAddress.formRule.validate(data) must
              be(Invalid(Seq(
                (Path \ "addressLine1") -> Seq(ValidationError("error.required.address.line1")),
                (Path \ "postCode") -> Seq(ValidationError("error.required.postcode"))
              )))
          }
          "non UK" in {
            val data = Map(
              "isUK" -> Seq("false"),
              "addressLineNonUK1" -> Seq(""),
              "country" -> Seq("")
            )

            PersonAddress.formRule.validate(data) must
              be(Invalid(Seq(
                (Path \ "addressLineNonUK1") -> Seq(ValidationError("error.required.address.line1")),
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
              (Path \ "addressLine1") -> Seq(ValidationError("error.required.enter.addresslineone.charcount")),
              (Path \ "addressLine2") -> Seq(ValidationError("error.required.enter.addresslinetwo.charcount")),
              (Path \ "addressLine3") -> Seq(ValidationError("error.required.enter.addresslinethree.charcount")),
              (Path \ "addressLine4") -> Seq(ValidationError("error.required.enter.addresslinefour.charcount")),
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
              (Path \ "addressLineNonUK1") -> Seq(ValidationError("error.required.enter.addresslineone.charcount")),
              (Path \ "addressLineNonUK2") -> Seq(ValidationError("error.required.enter.addresslinetwo.charcount")),
              (Path \ "addressLineNonUK3") -> Seq(ValidationError("error.required.enter.addresslinethree.charcount")),
              (Path \ "addressLineNonUK4") -> Seq(ValidationError("error.required.enter.addresslinefour.charcount")),
              (Path \ "country") -> Seq(ValidationError("error.invalid.country"))
            )))
        }
      }
      "there is invalid data" in {
        val model = defaultNonUKModel ++ Map("isUK" -> Seq("HGHHHH"))
        PersonAddress.formRule.validate(model) must be(
          Invalid(Seq(
            (Path \ "isUK") -> Seq(ValidationError("error.required.uk.or.overseas"))
          )))
      }
    }

    "write correct UK Address" in {
      PersonAddress.formWrites.writes(defaultUKAddress) must be(defaultUKModel)
    }

    "write correct Non UK Address" in {
      PersonAddress.formWrites.writes(defaultNonUKAddress) must be(defaultNonUKModel)
    }
  }

  "JSON validation" must {

    "Round trip a UK Address correctly through serialisation" in {
      PersonAddress.jsonReads.reads(
        PersonAddress.jsonWrites.writes(defaultUKAddress)
      ) must be(JsSuccess(defaultUKAddress))
    }

    "Round trip a Non UK Address correctly through serialisation" in {
      PersonAddress.jsonReads.reads(
        PersonAddress.jsonWrites.writes(defaultNonUKAddress)
      ) must be(JsSuccess(defaultNonUKAddress))
    }

    "Serialise UK address as expected" in {
      Json.toJson(defaultUKAddress.asInstanceOf[PersonAddress]) must be(defaultUKJson)
    }

    "Serialise non-UK address as expected" in {
      Json.toJson(defaultNonUKAddress.asInstanceOf[PersonAddress]) must be(defaultNonUKJson)
    }

    "Deserialise UK address as expected" in {
      defaultUKJson.as[PersonAddress] must be(defaultUKAddress)
    }

    "Deserialise non-UK address as expected" in {
      defaultNonUKJson.as[PersonAddress] must be(defaultNonUKAddress)
    }
  }

  "isComplete" must {

    val country = Country("Germany", "DE")

    "return true" when {

      "line 1 and postcode are non-empty for UK address" in {

        assert(PersonAddressUK(defaultAddressLine1, None, None, None, defaultPostcode).isComplete)
        assert(PersonAddressUK(defaultAddressLine1, defaultAddressLine2, defaultAddressLine3, defaultAddressLine4, defaultPostcode).isComplete)
      }

      "line 1, country code and country name are non-empty for non-UK address" in {

        assert(PersonAddressNonUK(defaultAddressLine1, None, None, None, country).isComplete)
        assert(PersonAddressNonUK(defaultAddressLine1, defaultAddressLine2, defaultAddressLine3, defaultAddressLine4, country).isComplete)
      }
    }

    "return false" when {

      "line 1 is empty" when {

        "address is UK" in {

          assert(!PersonAddressUK("", defaultAddressLine2, defaultAddressLine3, defaultAddressLine4, defaultPostcode).isComplete)
        }

        "address is non-UK" in {

          assert(!PersonAddressNonUK("", defaultAddressLine2, defaultAddressLine3, defaultAddressLine4, country).isComplete)
        }
      }

      "postcode is empty for UK address" in {

        assert(!PersonAddressUK(defaultAddressLine1, defaultAddressLine2, defaultAddressLine3, defaultAddressLine4, "").isComplete)
      }

      "country code is empty for non-UK address" in {

        assert(!PersonAddressNonUK(defaultAddressLine1, defaultAddressLine2, defaultAddressLine3, defaultAddressLine4, Country("Germany", "")).isComplete)
      }

      "country name is empty for non-UK address" in {

        assert(!PersonAddressNonUK(defaultAddressLine1, defaultAddressLine2, defaultAddressLine3, defaultAddressLine4, Country("", "DE")).isComplete)
      }
    }
  }
}
