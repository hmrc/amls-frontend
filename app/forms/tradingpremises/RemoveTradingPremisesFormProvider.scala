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
import models.tradingpremises.ActivityEndDate
import java.time.LocalDate
import play.api.data.Form

import javax.inject.Inject

class RemoveTradingPremisesFormProvider @Inject() () extends Mappings {

  def apply(): Form[ActivityEndDate] = Form[ActivityEndDate](
    "endDate" -> localDate(
      oneInvalidKey = "error.invalid.tp.one",
      multipleInvalidKey = "error.invalid.tp.multiple",
      oneRequiredKey = "error.required.tp.one",
      twoRequiredKey = "error.required.tp.two",
      allRequiredKey = "error.required.tp.all",
      realDateKey = "error.invalid.tp.date.not.real"
    ).verifying(
      minDate(RemoveTradingPremisesFormProvider.minDate, "error.invalid.year.post1900"),
      maxDate(RemoveTradingPremisesFormProvider.maxDate, "error.invalid.tp.date.future")
    ).transform[ActivityEndDate](
      ActivityEndDate(_),
      _.endDate
    )
  )

}

object RemoveTradingPremisesFormProvider {
  val minDate: LocalDate = LocalDate.of(1900, 1, 1)
  val maxDate: LocalDate = LocalDate.now()
}
