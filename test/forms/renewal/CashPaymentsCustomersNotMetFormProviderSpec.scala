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
import models.renewal.CashPaymentsCustomerNotMet
import play.api.data.Form

class CashPaymentsCustomersNotMetFormProviderSpec extends BooleanFieldBehaviours[CashPaymentsCustomerNotMet] {

  override val form: Form[CashPaymentsCustomerNotMet] = new CashPaymentsCustomersNotMetFormProvider()()
  override val fieldName: String                      = "receiveCashPayments"
  override val errorMessage: String                   = "error.required.renewal.hvd.receive.cash.payments"

  "CashPaymentsCustomersNotMetFormProvider" must {
    behave like booleanFieldWithModel(CashPaymentsCustomerNotMet(true), CashPaymentsCustomerNotMet(false))
  }
}
