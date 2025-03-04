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

package forms.responsiblepeople.address

import forms.mappings.Mappings
import models.responsiblepeople.NewHomeDateOfChange
import java.time.LocalDate
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class NewHomeAddressDateOfChangeFormProvider @Inject() () extends Mappings {

  def apply(): Form[NewHomeDateOfChange] = Form[NewHomeDateOfChange](
    mapping(
      "dateOfChange" -> localDate(
        oneInvalidKey = "new.home.error.invalid.date.one",
        multipleInvalidKey = "new.home.error.invalid.date.multiple",
        oneRequiredKey = "new.home.error.required.date.one",
        twoRequiredKey = "new.home.error.required.date.two",
        allRequiredKey = "new.home.error.required.date.all",
        realDateKey = "new.home.error.required.date.fake"
      ).verifying(
        minDate(NewHomeAddressDateOfChangeFormProvider.minDate, "new.home.error.required.date.1900"),
        maxDate(NewHomeAddressDateOfChangeFormProvider.maxDate, "new.home.error.required.date.future")
      )
    )(x => NewHomeDateOfChange(Some(x)))(_.dateOfChange)
  )

}

object NewHomeAddressDateOfChangeFormProvider {

  val minDate: LocalDate = LocalDate.of(1900, 1, 1)

  val maxDate: LocalDate = LocalDate.now()
}
