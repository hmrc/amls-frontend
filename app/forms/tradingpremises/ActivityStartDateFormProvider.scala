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

package forms.tradingpremises

import forms.mappings.Mappings
import models.tradingpremises.ActivityStartDate
import java.time.LocalDate
import play.api.data.Form

import javax.inject.Inject

class ActivityStartDateFormProvider @Inject() () extends Mappings {

  def apply(): Form[ActivityStartDate] = Form[ActivityStartDate](
    "startDate" -> localDate(
      oneInvalidKey = "error.invalid.date.tp.one",
      multipleInvalidKey = "error.invalid.date.tp.multiple",
      oneRequiredKey = "error.required.tp.address.date.one",
      twoRequiredKey = "error.required.tp.address.date.two",
      allRequiredKey = "error.required.tp.address.date.all",
      realDateKey = "error.invalid.date.tp.not.real"
    ).verifying(
      minDate(ActivityStartDateFormProvider.minDate, "error.invalid.date.tp.after.1700"),
      maxDate(ActivityStartDateFormProvider.maxDate, "error.invalid.date.tp.before.2100")
    ).transform[ActivityStartDate](
      ActivityStartDate(_),
      _.startDate
    )
  )

}

object ActivityStartDateFormProvider {

  val minDate: LocalDate = LocalDate.of(1700, 1, 1)

  val maxDate: LocalDate = LocalDate.of(2099, 12, 31)
}
