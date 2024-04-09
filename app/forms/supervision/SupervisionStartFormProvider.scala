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
import models.supervision.SupervisionStart
import org.joda.time.LocalDate
import play.api.data.Form

import javax.inject.Inject

class SupervisionStartFormProvider @Inject()() extends Mappings {

  def apply(): Form[SupervisionStart] = Form[SupervisionStart](
    "startDate" -> jodaLocalDate(
      invalidKey = "error.supervision.start.invalid.date.not.real",
      allRequiredKey = "error.supervision.start.required.date.all",
      twoRequiredKey = "error.supervision.start.required.date.two",
      requiredKey = "error.supervision.start.required.date.one"
    ).verifying(
      jodaMinDate(SupervisionStartFormProvider.minDate, "error.supervision.start.invalid.date.after.1900"),
      jodaMaxDate(SupervisionStartFormProvider.maxDate, "error.supervision.start.invalid.date.future")
    ).transform[SupervisionStart](
      SupervisionStart(_), _.startDate
    )
  )

}

object SupervisionStartFormProvider {

  val minDate: LocalDate = new LocalDate(1900, 1, 1)

  def maxDate: LocalDate = LocalDate.now()
}
