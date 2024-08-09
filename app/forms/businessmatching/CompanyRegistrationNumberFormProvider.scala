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

package forms.businessmatching

import forms.mappings.Mappings
import models.businessmatching.CompanyRegistrationNumber
import play.api.data.Form

import javax.inject.Inject

class CompanyRegistrationNumberFormProvider @Inject()() extends Mappings {

  def apply(): Form[CompanyRegistrationNumber] = {

    val length = 8
    val registrationNumberRegex = "^[A-Z0-9]{8}$"

    Form[CompanyRegistrationNumber](
      "value" -> textAllowWhitespace("error.required.bm.registration.number")
        .verifying(
          firstError(
            correctLength(length, "error.invalid.bm.registration.number.length"),
            regexp(registrationNumberRegex, "error.invalid.bm.registration.number.allowed")
          )
        ).transform[CompanyRegistrationNumber](CompanyRegistrationNumber(_), _.companyRegistrationNumber)
    )
  }
}
