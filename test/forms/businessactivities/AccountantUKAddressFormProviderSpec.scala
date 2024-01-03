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

package forms.businessactivities

import forms.behaviours.AddressFieldBehaviours
import models.businessactivities.AccountantsAddress
import play.api.data.Form

class AccountantUKAddressFormProviderSpec extends AddressFieldBehaviours {

  val formProvider = new AccountantUKAddressFormProvider()

  override val form: Form[AccountantsAddress] = formProvider()

  override val maxLength: Int = formProvider.length

  override val regexString: String = formProvider.addressTypeRegex

  "AccountantUKAddressFormProvider" when {

    behave like formWithAddressFields(
      "error.required.address",
      "error.max.length.address",
      "error.text.validation.address"
    )

    behave like postcodeField(postcodeRegex)
  }

}
