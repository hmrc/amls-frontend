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

package forms.hvd

import forms.behaviours.{CheckboxFieldBehaviours, StringFieldBehaviours}
import forms.mappings.Constraints
import models.hvd.{ItemType, Products}
import models.hvd.Products.Other
import play.api.data.{Form, FormError}

class ProductsFormProviderSpec extends CheckboxFieldBehaviours with StringFieldBehaviours with Constraints {

  val formProvider         = new ProductsFormProvider()
  val form: Form[Products] = formProvider()

  val checkboxFieldName    = "products"
  val checkboxErrorMessage = "error.required.hvd.business.sell.atleast"

  val textFieldName = "otherDetails"

  "ProductsFormProvider" when {

    behave like checkboxFieldWithWrapper[ItemType, Products](
      form,
      checkboxFieldName,
      Products.all.filterNot(_.value == Other("").value),
      x => Products(Set(x)),
      x => Products(x.toSet),
      invalidError = FormError(s"$checkboxFieldName[0]", checkboxErrorMessage)
    )

    behave like mandatoryCheckboxField(form, checkboxFieldName, checkboxErrorMessage)

    s"$textFieldName is submitted" must {

      "bind when Other is selected" in {

        forAll(stringOfLengthGen(formProvider.length)) { otherDetails =>
          val result = form
            .bind(
              Map(
                checkboxFieldName -> Other("").toString,
                textFieldName     -> otherDetails
              )
            )
            .apply(textFieldName)
          result.value.value shouldBe otherDetails
        }
      }

      "be mandatory if Other is selected" in {

        val result = form.bind(
          Map(
            checkboxFieldName -> Other("").toString,
            textFieldName     -> ""
          )
        )

        result.value                shouldBe None
        result.error(textFieldName) shouldBe Some(
          FormError(textFieldName, "error.required.hvd.business.sell.other.details")
        )
      }

      s"not bind strings that are longer that ${formProvider.length}" in {

        forAll(stringsLongerThan(formProvider.length).suchThat(_.nonEmpty)) { longString =>
          val result = form.bind(
            Map(
              checkboxFieldName -> Other("").toString,
              textFieldName     -> longString
            )
          )

          result.value                shouldBe None
          result.error(textFieldName) shouldBe Some(
            FormError(textFieldName, "error.invalid.hvd.business.sell.other.details", Seq(formProvider.length))
          )
        }
      }

      "not bind invalid strings" in {

        forAll(stringsShorterThan(formProvider.length - 1).suchThat(_.nonEmpty), invalidCharForNames) {
          (detail, invalid) =>
            val result = form.bind(
              Map(
                checkboxFieldName -> Other("").toString,
                textFieldName     -> (detail + invalid)
              )
            )

            result.value                shouldBe None
            result.error(textFieldName) shouldBe Some(
              FormError(textFieldName, "error.invalid.hvd.business.sell.other.format", Seq(basicPunctuationRegex))
            )
        }
      }
    }
  }
}
