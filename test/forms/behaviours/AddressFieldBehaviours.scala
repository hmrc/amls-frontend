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

package forms.behaviours

import forms.mappings.Constraints
import models.Country
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

import collection.mutable.{Map => MutableMap}

trait AddressFieldBehaviours extends FieldBehaviours with Constraints {

  val form: Form[_]
  val maxLength: Int
  val regexString: String

  val addressLinesData: MutableMap[String, String] = MutableMap(
    "addressLine1" -> "123 Test Street",
    "addressLine2" -> "Test Village",
    "addressLine3" -> "Test City",
    "addressLine4" -> "Test County"
  )

  protected def validAddressLineGen(maxLength: Int): Gen[String] =
    alphaStringsShorterThan(maxLength + 1).suchThat(_.nonEmpty)

  protected def bindForm(formData: MutableMap[String, String]): Form[_] =
    form.bind(Map(formData.toSeq.sortBy(_._1): _*))

  def formWithAddressFields(requiredErrorKey: String, lengthErrorKey: String, regexErrorKey: String): Unit = {

    val addressLineList = (1 to 4).toList

    addressLineList foreach { line =>
      val fieldName = s"addressLine$line"

      s"Address Line $line is validated" must {

        val dataToBind: collection.mutable.Map[String, String] =
          collection.mutable.Map(
            addressLineList
              .filterNot(_ == line)
              .map { l =>
                s"addressLine$l" -> s"Fake Address Line $l"
              }
              .toMap
              .toSeq: _*
          ) += "country" -> "US"

        "must bind correctly" in {

          forAll(validAddressLineGen(maxLength).suchThat(_.nonEmpty)) { addressLine =>
            val formData: MutableMap[String, String] = dataToBind += (fieldName -> addressLine)
            val newForm                              = bindForm(formData)

            newForm(fieldName).value shouldBe Some(addressLine)
          }
        }

        if (line == 1) {

          behave like mandatoryField(
            form,
            fieldName,
            FormError(fieldName, s"$requiredErrorKey.line$line")
          )
        }

        behave like fieldWithMaxLength(
          maxLength,
          dataToBind,
          fieldName,
          FormError(fieldName, s"$lengthErrorKey.line$line", Seq(maxLength))
        )

        behave like fieldWithRegexValidation(
          maxLength,
          dataToBind,
          fieldName,
          FormError(fieldName, s"$regexErrorKey.line$line", Seq(regexString))
        )
      }
    }
  }

  def fieldWithMaxLength(
    maxLength: Int,
    extraData: MutableMap[String, String],
    fieldName: String,
    lengthError: FormError
  ): Unit =
    s"not bind strings longer than $maxLength characters" in {

      forAll(Gen.alphaNumStr.suchThat(_.length > maxLength)) { string =>
        val formData: MutableMap[String, String] = extraData += (fieldName -> string)
        val newForm                              = bindForm(formData)

        newForm(fieldName).errors shouldEqual Seq(lengthError)
      }
    }
  def fieldWithRegexValidation(
    length: Int,
    extraData: MutableMap[String, String],
    fieldName: String,
    regexError: FormError
  ): Unit =
    s"not bind strings that violate regex" in {

      forAll(validAddressLineGen(length).suchThat(_.nonEmpty), invalidCharForNames) { (line, invalidChar) =>
        val invalidLine                          = line.dropRight(1) + invalidChar
        val formData: MutableMap[String, String] = extraData += (fieldName -> invalidLine)
        val newForm                              = bindForm(formData)

        newForm(fieldName).errors shouldEqual Seq(regexError)
      }
    }

  def postcodeField(postcodeRegex: String): Unit = {

    val postcodeField = "postCode"

    "postcode is validated" must {

      "bind a valid postcode" in {

        forAll(postcodeGen) { postcode =>
          val formData: MutableMap[String, String] = addressLinesData += (postcodeField -> postcode)
          val newForm                              = bindForm(formData)

          newForm(postcodeField).value shouldBe Some(postcode)
        }
      }

      "bind a valid postcode with spaces" in {
        val validPostcodesWithSpaces = Seq("NW 1 8 YD", "NW 1 8 YD", " NW18 YD")

        validPostcodesWithSpaces.foreach { postcode =>
          val formDataValid: MutableMap[String, String] = addressLinesData += (postcodeField -> postcode)
          val newFormValid                              = bindForm(formDataValid)

          newFormValid(postcodeField).value shouldBe Some(postcode)
        }
      }

      "fail to bind" when {

        "postcode is empty" in {

          val formData: MutableMap[String, String] = addressLinesData += (postcodeField -> "")
          val newForm                              = bindForm(formData)

          newForm(postcodeField).error shouldBe Some(FormError(postcodeField, "error.required.postcode"))
        }

        "postcode is invalid" in {

          forAll(postcodeGen.suchThat(_.nonEmpty), invalidChar) { case (postcode: String, invalidChar: String) =>
            val invalidPostcode                      = postcode.dropRight(1) + invalidChar
            val formData: MutableMap[String, String] = addressLinesData += (postcodeField -> invalidPostcode)
            val newForm                              = bindForm(formData)

            newForm(postcodeField).error shouldBe Some(
              FormError(postcodeField, "error.invalid.postcode", Seq(postcodeRegex))
            )
          }
        }
      }
    }
  }

  def countryField(isUKErrorKey: String): Unit = {

    val countryField = "country"
    val uk           = Country("United Kingdom", "GB")

    "country is validated" must {

      "bind a valid country" in {

        forAll(Gen.oneOf(models.countries).suchThat(_.isUK == false)) { country =>
          val formData: MutableMap[String, String] = addressLinesData += (countryField -> country.code)
          val newForm                              = bindForm(formData)

          newForm(countryField).value shouldBe Some(country.code)
        }
      }

      "fail to bind" when {

        "the given country is UK" in {

          val formData: MutableMap[String, String] = addressLinesData += (countryField -> uk.code)
          val newForm                              = bindForm(formData)

          newForm(countryField).error shouldBe Some(FormError(countryField, isUKErrorKey))
        }

        "country is empty" in {

          val formData: MutableMap[String, String] = addressLinesData += (countryField -> "")
          val newForm                              = bindForm(formData)

          newForm(countryField).error shouldBe Some(FormError(countryField, "error.required.country"))
        }

        "country is invalid" in {

          val formData: MutableMap[String, String] = addressLinesData += (countryField -> "foo")
          val newForm                              = bindForm(formData)

          newForm(countryField).error shouldBe Some(FormError(countryField, "error.invalid.country"))
        }
      }
    }
  }
}
