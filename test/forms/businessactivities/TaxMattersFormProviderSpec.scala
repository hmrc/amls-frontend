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

import forms.behaviours.BooleanFieldBehaviours
import models.businessactivities.TaxMatters
import play.api.data.Form

class TaxMattersFormProviderSpec extends BooleanFieldBehaviours[TaxMatters] {

  override val form: Form[TaxMatters] = new TaxMattersFormProvider()()
  override val fieldName: String      = "manageYourTaxAffairs"
  override val errorMessage: String   = "error.required.ba.tax.matters"

  "AccountantIsUKAddressFormProvider" must {

    behave like booleanFieldWithModel(
      TaxMatters(true),
      TaxMatters(false)
    )
  }
}
