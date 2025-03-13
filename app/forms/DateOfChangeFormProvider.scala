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

package forms

import forms.mappings.Mappings
import models.DateOfChange
import java.time.LocalDate
import play.api.data.Form

import javax.inject.Inject

class DateOfChangeFormProvider @Inject() () extends Mappings {

  def apply(): Form[DateOfChange] = Form[DateOfChange](
    "dateOfChange" -> localDate(
      oneInvalidKey = "error.invalid.dateofchange.one",
      multipleInvalidKey = "error.invalid.dateofchange.multiple",
      oneRequiredKey = "error.required.dateofchange.one",
      twoRequiredKey = "error.required.dateofchange.two",
      allRequiredKey = "error.required.dateofchange.all",
      realDateKey = "error.required.dateofchange.not.real"
    ).verifying(
      minDate(DateOfChangeFormProvider.minDate, "error.invalid.dateofchange.before.1900"),
      maxDate(DateOfChangeFormProvider.maxDate, "error.invalid.dateofchange.future")
    ).transform[DateOfChange](
      DateOfChange(_),
      _.dateOfChange
    )
  )

}

object DateOfChangeFormProvider {

  val minDate: LocalDate = LocalDate.of(1900, 1, 1)

  def maxDate: LocalDate = LocalDate.now()
}
