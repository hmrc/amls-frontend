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
import models.responsiblepeople.PersonName
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class PersonNameFormProvider @Inject() () extends Mappings {

  val length = 35

  def apply(): Form[PersonName] = Form[PersonName](
    mapping(
      "firstName"  -> text("error.required.rp.first_name").verifying(
        firstError(
          maxLength(length, "error.invalid.rp.first_name.length"),
          regexp(nameRegex, "error.invalid.rp.first_name.validation")
        )
      ),
      "middleName" -> optional(
        text("error.required.rp.middle_name").verifying(
          firstError(
            maxLength(length, "error.invalid.rp.middle_name.length"),
            regexp(nameRegex, "error.invalid.rp.middle_name.validation")
          )
        )
      ),
      "lastName"   -> text("error.required.rp.last_name").verifying(
        firstError(
          maxLength(length, "error.invalid.rp.last_name.length"),
          regexp(nameRegex, "error.invalid.rp.last_name.validation")
        )
      )
    )(PersonName.apply)(PersonName.unapply)
  )
}
