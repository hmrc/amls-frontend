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

package forms.renewal

import forms.behaviours.BooleanFieldBehaviours
import models.renewal.CustomersOutsideIsUK
import play.api.data.Form

class CustomersOutsideIsUKFormProviderSpec extends BooleanFieldBehaviours[CustomersOutsideIsUK] {

  override val form: Form[CustomersOutsideIsUK] = new CustomersOutsideIsUKFormProvider()()
  override val fieldName: String                = "isOutside"
  override val errorMessage: String             = "error.required.ba.renewal.select.yes"

  "CustomersOutsideIsUKFormProvider" must {

    behave like booleanFieldWithModel(CustomersOutsideIsUK(true), CustomersOutsideIsUK(false))
  }
}
