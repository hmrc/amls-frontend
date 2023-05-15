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

package forms.supervision

import forms.mappings.Mappings
import models.supervision.SupervisionEnd
import org.joda.time.LocalDate
import play.api.data.Form

import javax.inject.Inject

class SupervisionEndFormProvider @Inject()() extends Mappings {

  def apply(): Form[SupervisionEnd] = Form[SupervisionEnd](
    "endDate" -> jodaLocalDate(
      invalidKey = "error.supervision.end.invalid.date.not.real",
      allRequiredKey = "error.supervision.end.required.date.all",
      twoRequiredKey = "error.supervision.end.required.date.two",
      requiredKey = "error.supervision.end.required.date.one"
    ).verifying(
      jodaMinDate(SupervisionEndFormProvider.minDate, "error.supervision.end.invalid.date.after.1900"),
      jodaMaxDate(SupervisionEndFormProvider.maxDate, "error.supervision.end.invalid.date.future")
    ).transform[SupervisionEnd](
      SupervisionEnd(_), _.endDate
    )
  )

}

object SupervisionEndFormProvider {

  val minDate: LocalDate = new LocalDate(1900, 1, 1)

  val maxDate: LocalDate = LocalDate.now()
}
