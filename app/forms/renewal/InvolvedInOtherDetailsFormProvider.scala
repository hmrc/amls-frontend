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

package forms.renewal

import forms.mappings.Mappings
import models.renewal.InvolvedInOtherYes
import play.api.data.Form

import javax.inject.Inject

class InvolvedInOtherDetailsFormProvider @Inject() () extends Mappings {

  val detailsMaxLength = 255

  def apply(): Form[InvolvedInOtherYes] = Form[InvolvedInOtherYes](
    "details" -> text("error.required.renewal.ba.involved.in.other.text")
      .verifying(
        firstError(
          maxLength(detailsMaxLength, "error.invalid.maxlength.255.renewal.ba.involved.in.other"),
          regexp(basicPunctuationRegex, "error.text.validation.renewal.ba.involved.in.other")
        )
      )
      .transform[InvolvedInOtherYes](InvolvedInOtherYes.apply, _.details)
  )
}
