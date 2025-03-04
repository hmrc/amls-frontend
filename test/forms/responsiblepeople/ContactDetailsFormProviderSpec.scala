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

package forms.responsiblepeople

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import play.api.data.FormError

class ContactDetailsFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider = new ContactDetailsFormProvider()
  val form         = formProvider()

  val phoneFormField = "phoneNumber"
  val emailFormField = "emailAddress"

  "ContactDetailsFormProvider" when {

    s"$phoneFormField is populated" must {

      behave like fieldThatBindsValidData(
        form,
        phoneFormField,
        numStringOfLength(formProvider.phoneLength).suchThat(_.nonEmpty)
      )

      behave like mandatoryField(
        form,
        phoneFormField,
        FormError(phoneFormField, "error.required.rp.contact.phone.number")
      )

      behave like fieldWithMaxLength(
        form,
        phoneFormField,
        formProvider.phoneLength,
        FormError(phoneFormField, "error.invalid.rp.contact.phone.number", Seq(formProvider.phoneLength))
      )

      "fail to bind when regex is violated" in {

        forAll(alphaStringsShorterThan(formProvider.phoneLength).suchThat(_.nonEmpty)) { str =>
          val result = form.bind(
            Map(
              phoneFormField -> str,
              emailFormField -> "fake.email@gmail.com"
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(phoneFormField, "error.invalid.rp.contact.phone.number", Seq("^[0-9 ()+\u2010\u002d]{1,24}$"))
          )
        }
      }
    }

    s"$emailFormField is populated" must {

      behave like fieldThatBindsValidData(
        form,
        emailFormField,
        emailGen
      )

      behave like mandatoryField(form, emailFormField, FormError(emailFormField, "error.required.rp.contact.email"))

      behave like fieldWithMaxLength(
        form,
        emailFormField,
        formProvider.emailLength,
        FormError(emailFormField, "error.invalid.rp.contact.email.length", Seq(formProvider.emailLength))
      )

      "fail to bind when regex is violated" in {

        forAll(emailGen) { email =>
          val result = form.bind(
            Map(
              emailFormField -> (email + "§"),
              phoneFormField -> "07123456789"
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(emailFormField, "error.invalid.rp.contact.email", Seq(emailRegex)))
        }
      }
    }
  }
}
