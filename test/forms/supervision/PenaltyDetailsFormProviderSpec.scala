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

package forms.supervision

import forms.behaviours.FieldBehaviours
import forms.mappings.Constraints
import models.supervision.ProfessionalBodyYes
import play.api.data.{Form, FormError}

class PenaltyDetailsFormProviderSpec extends FieldBehaviours with Constraints {

  val formProvider: PenaltyDetailsFormProvider = new PenaltyDetailsFormProvider()

  val form: Form[ProfessionalBodyYes] = formProvider()
  val fieldName: String               = "professionalBody"

  "form" must {

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsShorterThan(formProvider.length)
    )

    behave like mandatoryField(
      form,
      fieldName,
      FormError(fieldName, "error.required.penaltydetails.info.about.penalty")
    )

    "details are submitted but max length exceeded" in {

      forAll(stringsLongerThan(formProvider.length + 1)) { longDetails =>
        val boundForm = form.bind(Map(fieldName -> longDetails))
        boundForm.errors.headOption shouldBe Some(
          FormError(fieldName, "error.invalid.penaltydetails.info.about.penalty.length.255", Seq(formProvider.length))
        )
      }
    }

    "is invalid" in {

      val boundForm = form.bind(Map(fieldName -> "§"))
      boundForm.errors.headOption shouldBe Some(
        FormError(fieldName, "error.invalid.penaltydetails.info.about.penalty", Seq(basicPunctuationRegex))
      )
    }
  }
}
