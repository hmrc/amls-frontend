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

import forms.behaviours.BooleanFieldBehaviours
import models.businessdetails.{PreviouslyRegistered, PreviouslyRegisteredNo, PreviouslyRegisteredYes}
import play.api.data.Form

class PreviouslyRegisteredFormProviderSpec extends BooleanFieldBehaviours[PreviouslyRegistered] {

  override val form: Form[PreviouslyRegistered] = new PreviouslyRegisteredFormProvider()()
  override val fieldName: String                = "value"
  override val errorMessage: String             = "error.required.atb.previously.registered"

  "PreviouslyRegistered form" must {
    behave like booleanFieldWithModel(PreviouslyRegisteredYes(None), PreviouslyRegisteredNo)
  }
}
