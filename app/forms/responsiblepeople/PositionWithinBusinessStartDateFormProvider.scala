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
import models.responsiblepeople.PositionStartDate
import java.time.LocalDate
import play.api.data.Form

import javax.inject.Inject

class PositionWithinBusinessStartDateFormProvider @Inject()() extends Mappings {

  def apply(): Form[PositionStartDate] = Form[PositionStartDate](
    "startDate" -> localDate(
      invalidKey = "error.rp.position.invalid.date.not.real",
      allRequiredKey = "error.rp.position.required.date.all",
      twoRequiredKey = "error.rp.position.required.date.two",
      requiredKey = "error.rp.position.required.date.one"
    ).verifying(
      minDate(PositionWithinBusinessStartDateFormProvider.minDate, "error.rp.position.invalid.date.after.1900"),
      maxDate(PositionWithinBusinessStartDateFormProvider.maxDate, "error.rp.position.invalid.date.future")
    ).transform[PositionStartDate](
      PositionStartDate(_), _.startDate
    )
  )

}

object PositionWithinBusinessStartDateFormProvider {

  val minDate: LocalDate = LocalDate.of(1900, 1, 1)

  def maxDate: LocalDate = LocalDate.now()
}

