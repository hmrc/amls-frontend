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

import forms.mappings.Mappings
import models.supervision.ProfessionalBodyYes
import play.api.data.Form

import javax.inject.Inject

class PenaltyDetailsFormProvider @Inject() () extends Mappings {

  val length = 255

  def apply(): Form[ProfessionalBodyYes] = Form[ProfessionalBodyYes](
    "professionalBody" -> text("error.required.penaltydetails.info.about.penalty")
      .verifying(
        firstError(
          maxLength(length, "error.invalid.penaltydetails.info.about.penalty.length.255"),
          regexp(basicPunctuationRegex, "error.invalid.penaltydetails.info.about.penalty")
        )
      )
      .transform[ProfessionalBodyYes](ProfessionalBodyYes.apply, _.value)
  )
}
