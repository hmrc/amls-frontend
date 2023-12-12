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

import forms.behaviours.AddressFieldBehaviours
import models.responsiblepeople.{NewHomeAddress, ResponsiblePersonCurrentAddress}
import play.api.data.Form

class NewHomeAddressNonUKFormProviderSpec extends AddressFieldBehaviours {

  val formProvider = new NewHomeAddressNonUKFormProvider()

  override val form: Form[NewHomeAddress] = formProvider()

  override val maxLength: Int = formProvider.length

  override val regexString: String = formProvider.addressTypeRegex

  "NewHomeAddressNonUKFormProviderSpec" when {

    behave like formWithAddressFields(
      "error.required.address",
      "error.max.length.address",
      "error.text.validation.address"
    )

    behave like countryField("error.required.enter.non.uk")
  }
}
