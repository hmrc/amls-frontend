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

package forms.deregister

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.deregister.DeregistrationReason
import models.withdrawal.WithdrawalReason.Other
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class DeregistrationReasonFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider: DeregistrationReasonFormProvider = new DeregistrationReasonFormProvider()
  val form: Form[DeregistrationReason]               = formProvider()

  val radio     = "deregistrationReason"
  val textField = "specifyOtherReason"

  "DeregistrationReasonFormProvider" when {

    "withdrawal reason is submitted" must {

      behave like fieldThatBindsValidData(form, radio, Gen.oneOf(DeregistrationReason.all.map(_.toString)))

      behave like mandatoryField(form, radio, FormError(radio, "error.required.deregistration.reason"))
    }

    "specify other reason is submitted" must {

      "bind valid strings" in {

        forAll(stringOfLengthGen(formProvider.length)) { reason =>
          val result = form
            .bind(
              Map(
                radio     -> Other("").toString,
                textField -> reason
              )
            )
            .apply(textField)
          result.value.value shouldBe reason
        }
      }

      "be mandatory if Other is selected" in {

        val result = form.bind(
          Map(
            radio     -> Other("").toString,
            textField -> ""
          )
        )

        result.value            shouldBe None
        result.error(textField) shouldBe Some(FormError(textField, "error.required.deregistration.reason.input"))
      }

      s"not bind strings that are longer that ${formProvider.length}" in {

        forAll(stringsLongerThan(formProvider.length).suchThat(_.nonEmpty)) { longString =>
          val result = form.bind(
            Map(
              radio     -> Other("").toString,
              textField -> longString
            )
          )

          result.value            shouldBe None
          result.error(textField) shouldBe Some(
            FormError(textField, "error.required.deregistration.reason.length", Seq(formProvider.length))
          )
        }
      }

      "not bind invalid strings" in {

        forAll(stringsShorterThan(formProvider.length).suchThat(_.nonEmpty), invalidCharForNames) {
          (software, invalidChar) =>
            val result = form.bind(
              Map(
                radio     -> Other("").toString,
                textField -> (software + invalidChar)
              )
            )

            result.value            shouldBe None
            result.error(textField) shouldBe Some(
              FormError(textField, "error.required.deregistration.reason.format", Seq(basicPunctuationRegex))
            )
        }
      }
    }
  }
}
