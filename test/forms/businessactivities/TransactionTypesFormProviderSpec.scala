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

package forms.businessactivities

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.businessactivities.TransactionTypes
import models.businessactivities.TransactionTypes.DigitalOther
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class TransactionTypesFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider: TransactionTypesFormProvider = new TransactionTypesFormProvider()
  val form: Form[TransactionTypes]               = formProvider()

  val checkboxField = "types"
  val textField     = "software"

  "TransactionTypesFormProvider" when {

    "types is submitted" must {

      behave like fieldThatBindsValidData(form, checkboxField, Gen.oneOf(TransactionTypes.all.map(_.toString)))

      behave like mandatoryField(
        form,
        checkboxField,
        FormError(checkboxField, "error.required.ba.atleast.one.transaction.record")
      )
    }

    "software is submitted" must {

      "bind valid strings" in {

        forAll(stringOfLengthGen(formProvider.length)) { software =>
          val result = form
            .bind(
              Map(
                checkboxField -> DigitalOther.toString,
                textField     -> software
              )
            )
            .apply(textField)
          result.value.value shouldBe software
        }
      }

      "be mandatory if Digital Software is selected" in {

        val result = form.bind(
          Map(
            "types[3]" -> DigitalOther.toString,
            textField  -> ""
          )
        )

        result.value            shouldBe None
        result.error(textField) shouldBe Some(FormError(textField, "error.required.ba.software.package.name"))
      }

      s"not bind strings that are longer that ${formProvider.length}" in {

        forAll(stringsLongerThan(formProvider.length).suchThat(_.nonEmpty)) { longString =>
          val result = form.bind(
            Map(
              "types[3]" -> DigitalOther.toString,
              textField  -> longString
            )
          )

          result.value            shouldBe None
          result.error(textField) shouldBe Some(
            FormError(textField, "error.max.length.ba.software.package.name", Seq(formProvider.length))
          )
        }
      }

      "not bind invalid strings" in {

        forAll(stringsShorterThan(formProvider.length).suchThat(_.nonEmpty), invalidCharForNames) {
          (software, invalidChar) =>
            val result = form.bind(
              Map(
                "types[3]" -> DigitalOther.toString,
                textField  -> (software + invalidChar)
              )
            )

            result.value            shouldBe None
            result.error(textField) shouldBe Some(
              FormError(textField, "error.invalid.characters.ba.software.package.name", Seq(basicPunctuationRegex))
            )
        }
      }
    }
  }
}
