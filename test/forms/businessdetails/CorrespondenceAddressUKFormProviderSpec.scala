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

package forms.businessdetails

import forms.behaviours.AddressFieldBehaviours
import models.businessdetails.CorrespondenceAddressUk
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

import scala.collection.mutable.{Map => MutableMap}

class CorrespondenceAddressUKFormProviderSpec extends AddressFieldBehaviours {

  val formProvider = new CorrespondenceAddressUKFormProvider()

  override val form: Form[CorrespondenceAddressUk] = formProvider()

  override val maxLength: Int = formProvider.length

  override val regexString: String = formProvider.addressTypeRegex

  val yourNameField     = "yourName"
  val businessNameField = "businessName"

  val yourNameGen: Gen[String]     = validAddressLineGen(formProvider.nameMaxLength).suchThat(_.nonEmpty)
  val businessNameGen: Gen[String] = validAddressLineGen(formProvider.businessNameMaxLength).suchThat(_.nonEmpty)

  val extraDataForYourName: MutableMap[String, String]     = addressLinesData += (businessNameField -> "Big Corp Inc")
  val extraDataForBusinessName: MutableMap[String, String] = addressLinesData += (yourNameField     -> "John Doe")

  "CorrespondenceAddressUKFormProvider" when {

    "yourName is validated" must {

      "bind when valid data is submitted" in {

        forAll(yourNameGen) { name =>
          val formData = extraDataForYourName += (yourNameField -> name)
          val result   = bindForm(formData)(yourNameField)

          result.value  shouldBe Some(name)
          result.errors shouldBe Nil
        }
      }

      behave like mandatoryField(
        form,
        yourNameField,
        FormError(yourNameField, "error.required.yourname")
      )

      s"not bind strings longer than ${formProvider.nameMaxLength} characters" in {

        forAll(Gen.alphaStr.suchThat(_.length > formProvider.nameMaxLength)) { string =>
          val formData: MutableMap[String, String] = extraDataForYourName += (yourNameField -> string)
          val newForm                              = bindForm(formData)

          newForm(yourNameField).errors shouldEqual Seq(
            FormError(yourNameField, "error.invalid.yourname", Seq(formProvider.nameMaxLength))
          )
        }
      }

      "not bind strings that violate regex" in {

        forAll(yourNameGen, invalidCharForNames.suchThat(_.nonEmpty)) { (line, invalidChar) =>
          val invalidLine                          = line.dropRight(1) + invalidChar
          val formData: MutableMap[String, String] = extraDataForYourName += (yourNameField -> invalidLine)
          val newForm                              = bindForm(formData)

          newForm(yourNameField).errors shouldEqual Seq(
            FormError(yourNameField, "error.invalid.yourname.validation", Seq(formProvider.regex))
          )
        }
      }
    }

    "businessName is validated" must {

      "bind when valid data is submitted" in {

        forAll(businessNameGen) { name =>
          val formData = extraDataForBusinessName += (businessNameField -> name)
          val result   = bindForm(formData)(businessNameField)
          result.value  shouldBe Some(name)
          result.errors shouldBe Nil
        }
      }

      behave like mandatoryField(
        form,
        businessNameField,
        FormError(businessNameField, "error.required.name.of.business")
      )

      s"not bind strings longer than ${formProvider.businessNameMaxLength} characters" in {

        forAll(Gen.alphaStr.suchThat(_.length > formProvider.businessNameMaxLength)) { string =>
          val formData: MutableMap[String, String] = extraDataForBusinessName += (businessNameField -> string)
          val newForm                              = bindForm(formData)

          newForm(businessNameField).errors shouldEqual Seq(
            FormError(businessNameField, "error.invalid.name.of.business", Seq(formProvider.businessNameMaxLength))
          )
        }
      }

      "not bind strings that violate regex" in {

        forAll(businessNameGen, invalidCharForNames.suchThat(_.nonEmpty)) { (line, invalidChar) =>
          val invalidLine                          = line.dropRight(1) + invalidChar
          val formData: MutableMap[String, String] = extraDataForBusinessName += (businessNameField -> invalidLine)
          val newForm                              = bindForm(formData)

          newForm(businessNameField).errors shouldEqual Seq(
            FormError(businessNameField, "error.invalid.name.of.business.validation", Seq(formProvider.regex))
          )
        }
      }
    }

    behave like formWithAddressFields(
      "error.required.address",
      "error.max.length.address",
      "error.text.validation.address"
    )

    behave like postcodeField(postcodeRegex)
  }
}
