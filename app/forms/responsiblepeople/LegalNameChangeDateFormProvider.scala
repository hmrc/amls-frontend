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

import forms.mappings.Mappings
import models.responsiblepeople.LegalNameChangeDate
import models.supervision.SupervisionEnd
import org.joda.time.LocalDate
import play.api.data.Form

import javax.inject.Inject

class LegalNameChangeDateFormProvider @Inject()() extends Mappings {

  def apply(): Form[LegalNameChangeDate] = Form[LegalNameChangeDate](
    "date" -> jodaLocalDate(
      invalidKey = "error.rp.name_change.invalid.date.not.real",
      allRequiredKey = "error.rp.name_change.required.date.all",
      twoRequiredKey = "error.rp.name_change.required.date.two",
      requiredKey = "error.rp.name_change.required.date.one"
    ).verifying(
      jodaMinDate(LegalNameChangeDateFormProvider.minDate, "error.rp.name_change.invalid.date.after.1900"),
      jodaMaxDate(LegalNameChangeDateFormProvider.maxDate, "error.rp.name_change.invalid.date.future")
    ).transform[LegalNameChangeDate](
      LegalNameChangeDate(_), _.date
    )
  )

}

object LegalNameChangeDateFormProvider {

  val minDate: LocalDate = new LocalDate(1900, 1, 1)

  val maxDate: LocalDate = LocalDate.now()
}
