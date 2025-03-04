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

package forms.businessdetails

import forms.mappings.Mappings
import models.businessdetails.ActivityStartDate
import java.time.LocalDate
import play.api.data.Form

import javax.inject.Inject

class ActivityStartDateFormProvider @Inject() () extends Mappings {

  def apply(): Form[ActivityStartDate] = Form[ActivityStartDate](
    "value" -> localDate(
      oneInvalidKey = "error.invalid.date.one",
      multipleInvalidKey = "error.invalid.date.multiple",
      oneRequiredKey = "error.required.date.required.one",
      twoRequiredKey = "error.required.date.required.two",
      allRequiredKey = "error.required.date.required.all",
      realDateKey = "error.invalid.date.not.real"
    ).verifying(
      minDate(ActivityStartDateFormProvider.minDate, "error.invalid.date.after.1900"),
      maxDate(ActivityStartDateFormProvider.maxDate, "error.invalid.date.before.2100")
    ).transform[ActivityStartDate](
      ActivityStartDate(_),
      _.startDate
    )
  )

}

object ActivityStartDateFormProvider {

  val minDate: LocalDate = LocalDate.of(1900, 1, 1)

  val maxDate: LocalDate = LocalDate.of(2099, 12, 31)
}
