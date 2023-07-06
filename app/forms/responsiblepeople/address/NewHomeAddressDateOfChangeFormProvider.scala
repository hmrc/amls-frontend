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

package forms.responsiblepeople.address

import forms.mappings.Mappings
import models.responsiblepeople.{DateOfBirth, NewHomeDateOfChange}
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class NewHomeAddressDateOfChangeFormProvider @Inject()() extends Mappings {

  def apply(): Form[NewHomeDateOfChange] = Form[NewHomeDateOfChange](
    mapping(
      "dateOfChange" -> jodaLocalDate(
        invalidKey = "new.home.error.required.date.fake",
        allRequiredKey = "new.home.error.required.date.all",
        twoRequiredKey = "new.home.error.required.date.two",
        requiredKey = "new.home.error.required.date.one"
      ).verifying(
        jodaMinDate(NewHomeAddressDateOfChangeFormProvider.minDate, "new.home.error.required.date.1900"),
        jodaMaxDate(NewHomeAddressDateOfChangeFormProvider.maxDate, "new.home.error.required.date.future")
      )
    )(x => NewHomeDateOfChange(Some(x)))(_.dateOfChange)
  )

}

object NewHomeAddressDateOfChangeFormProvider {

  val minDate: LocalDate = new LocalDate(1900, 1, 1)

  val maxDate: LocalDate = LocalDate.now()
}
