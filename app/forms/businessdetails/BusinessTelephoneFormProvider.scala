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
import models.businessdetails.ContactingYouPhone
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class BusinessTelephoneFormProvider @Inject() () extends Mappings {

  val length                            = 24
  val regex                             = "^[0-9 ()+\u2010\u002d]{1,24}$"
  def apply(): Form[ContactingYouPhone] = Form[ContactingYouPhone](
    mapping(
      "phoneNumber" -> text("error.required.phone.number").verifying(
        firstError(
          maxLength(length, "error.max.length.phone"),
          regexp(regex, "err.invalid.phone.number")
        )
      )
    )(ContactingYouPhone.apply)(ContactingYouPhone.unapply)
  )
}
