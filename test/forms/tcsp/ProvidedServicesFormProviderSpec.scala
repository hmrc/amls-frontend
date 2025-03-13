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

package forms.tcsp

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.tcsp.ProvidedServices
import models.tcsp.ProvidedServices.Other
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class ProvidedServicesFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider: ProvidedServicesFormProvider = new ProvidedServicesFormProvider()
  val form: Form[ProvidedServices]               = formProvider()

  val checkboxField = "services"
  val textField     = "details"

  "ProvidedServicesFormProvider" when {

    "services is submitted" must {

      behave like fieldThatBindsValidData(
        form,
        checkboxField,
        Gen.oneOf(ProvidedServices.all.filterNot(_ == Other("")).map(_.toString))
      )

      behave like mandatoryField(
        form,
        checkboxField,
        FormError(checkboxField, "error.required.tcsp.provided_services.services")
      )
    }

    "details is submitted" must {

      "bind valid strings" in {

        forAll(stringOfLengthGen(formProvider.length)) { detail =>
          val result = form
            .bind(
              Map(
                checkboxField -> Other("").toString,
                textField     -> detail
              )
            )
            .apply(textField)
          result.value.value shouldBe detail
        }
      }

      "be mandatory if Other is selected" in {

        val result = form.bind(
          Map(
            checkboxField -> Other("").toString,
            textField     -> ""
          )
        )

        result.value            shouldBe None
        result.error(textField) shouldBe Some(FormError(textField, "error.required.tcsp.provided_services.details"))
      }

      s"not bind strings that are longer that ${formProvider.length}" in {

        forAll(stringsLongerThan(formProvider.length).suchThat(_.nonEmpty)) { longString =>
          val result = form.bind(
            Map(
              checkboxField -> Other("").toString,
              textField     -> longString
            )
          )

          result.value            shouldBe None
          result.error(textField) shouldBe Some(
            FormError(textField, "error.required.tcsp.provided_services.details.length", Seq(formProvider.length))
          )
        }
      }

      "not bind invalid strings" in {

        forAll(stringsShorterThan(formProvider.length - 1).suchThat(_.nonEmpty), invalidCharForNames) {
          (detail, invalid) =>
            val result = form.bind(
              Map(
                checkboxField -> Other("").toString,
                textField     -> (detail + invalid)
              )
            )

            result.value            shouldBe None
            result.error(textField) shouldBe Some(
              FormError(
                textField,
                "error.required.tcsp.provided_services.details.punctuation",
                Seq(basicPunctuationRegex)
              )
            )
        }
      }
    }
  }
}
