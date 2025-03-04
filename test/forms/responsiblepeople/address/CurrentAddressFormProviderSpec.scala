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

import forms.behaviours.BooleanFieldBehaviours
import models.Country
import models.responsiblepeople.{PersonAddressNonUK, PersonAddressUK, ResponsiblePersonCurrentAddress}
import play.api.data.Form

class CurrentAddressFormProviderSpec extends BooleanFieldBehaviours[ResponsiblePersonCurrentAddress] {

  override val form: Form[ResponsiblePersonCurrentAddress] = new CurrentAddressFormProvider()()
  override val fieldName: String                           = "isUK"
  override val errorMessage: String                        = "error.required.uk.or.overseas.address.current"

  "CurrentAddressFormProvider" must {

    behave like booleanFieldWithModel(
      ResponsiblePersonCurrentAddress(PersonAddressUK("", None, None, None, ""), None, None),
      ResponsiblePersonCurrentAddress(PersonAddressNonUK("", None, None, None, Country("", "")), None)
    )
  }
}
