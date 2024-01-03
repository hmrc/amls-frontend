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

package forms.msb

import forms.behaviours.BooleanFieldBehaviours
import models.moneyservicebusiness.{UsesForeignCurrencies, UsesForeignCurrenciesNo, UsesForeignCurrenciesYes}
import play.api.data.Form

class UsesForeignCurrenciesFormProviderSpec extends BooleanFieldBehaviours[UsesForeignCurrencies] {
  override val form: Form[UsesForeignCurrencies] = new UsesForeignCurrenciesFormProvider()()
  override val fieldName: String = "usesForeignCurrencies"
  override val errorMessage: String = "error.required.msb.wc.foreignCurrencies"

  "UsesForeignCurrenciesFormProvider" must {

    behave like booleanFieldWithModel(
      UsesForeignCurrenciesYes, UsesForeignCurrenciesNo
    )
  }
}
