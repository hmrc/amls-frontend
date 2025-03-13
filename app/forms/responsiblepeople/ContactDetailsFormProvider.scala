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
import models.responsiblepeople.ContactDetails
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class ContactDetailsFormProvider @Inject() () extends Mappings {

  val phoneLength = 24
  val emailLength = 100

  private val phoneInvalidError     = "error.invalid.rp.contact.phone.number"
  private val phoneRegex            = "^[0-9 ()+\u2010\u002d]{1,24}$"
  def apply(): Form[ContactDetails] = Form[ContactDetails](
    mapping(
      "phoneNumber"  -> text("error.required.rp.contact.phone.number")
        .verifying(
          firstError(
            maxLength(phoneLength, phoneInvalidError),
            regexp(phoneRegex, phoneInvalidError)
          )
        ),
      "emailAddress" -> text("error.required.rp.contact.email")
        .verifying(
          firstError(
            maxLength(emailLength, "error.invalid.rp.contact.email.length"),
            regexp(emailRegex, "error.invalid.rp.contact.email")
          )
        )
    )(ContactDetails.apply)(ContactDetails.unapply)
  )
}
