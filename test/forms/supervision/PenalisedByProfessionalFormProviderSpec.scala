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

import forms.behaviours.BooleanFieldBehaviours
import forms.mappings.Constraints
import models.supervision.ProfessionalBody
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class PenalisedByProfessionalFormProviderSpec extends BooleanFieldBehaviours[ProfessionalBody] with Constraints {

  val formProvider: PenalisedByProfessionalFormProvider = new PenalisedByProfessionalFormProvider()

  override val form: Form[ProfessionalBody] = formProvider()
  override val fieldName: String            = "penalised"
  override val errorMessage: String         = "error.required.professionalbody.penalised.by.professional.body"

  "form" must {

    behave like fieldThatBindsValidData(form, fieldName, Gen.oneOf(Seq("true", "false")))

    behave like mandatoryField(form, fieldName, FormError(fieldName, errorMessage))
  }
}
