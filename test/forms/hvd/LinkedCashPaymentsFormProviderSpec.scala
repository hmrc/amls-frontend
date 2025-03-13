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

package forms.hvd

import forms.behaviours.BooleanFieldBehaviours
import models.hvd.LinkedCashPayments
import play.api.data.Form

class LinkedCashPaymentsFormProviderSpec extends BooleanFieldBehaviours[LinkedCashPayments] {

  override val form: Form[LinkedCashPayments] = new LinkedCashPaymentsFormProvider()()
  override val fieldName: String              = "linkedCashPayments"
  override val errorMessage: String           = "error.required.hvd.linked.cash.payment"

  "LinkedCashPaymentsFormProvider" must {

    behave like booleanFieldWithModel(
      LinkedCashPayments(true),
      LinkedCashPayments(false)
    )
  }
}
