/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.mappings.Mappings
import models.responsiblepeople.DateOfBirth
import org.joda.time.LocalDate
import play.api.data.Form

import javax.inject.Inject

class DateOfBirthFormProvider @Inject()() extends Mappings {

  def apply(): Form[DateOfBirth] = Form[DateOfBirth](
    "dateOfBirth" -> jodaLocalDate(
      invalidKey = "error.rp.dob.invalid.date.not.real",
      allRequiredKey = "error.rp.dob.required.date.all",
      twoRequiredKey = "error.rp.dob.required.date.two",
      requiredKey = "error.rp.dob.required.date.one"
    ).verifying(
      jodaMinDate(LegalNameChangeDateFormProvider.minDate, "error.rp.dob.invalid.date.after.1900"),
      jodaMaxDate(LegalNameChangeDateFormProvider.maxDate, "error.rp.dob.invalid.date.future")
    ).transform[DateOfBirth](
      DateOfBirth(_), _.dateOfBirth
    )
  )

}

object DateOfBirthFormProvider {

  val minDate: LocalDate = new LocalDate(1900, 1, 1)

  val maxDate: LocalDate = LocalDate.now()
}
