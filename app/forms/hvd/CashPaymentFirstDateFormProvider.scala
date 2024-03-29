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

import forms.mappings.Mappings
import models.hvd.CashPaymentFirstDate
import org.joda.time.LocalDate
import play.api.data.Form

import javax.inject.Inject

class CashPaymentFirstDateFormProvider @Inject()() extends Mappings {

  def apply(): Form[CashPaymentFirstDate] = Form[CashPaymentFirstDate](
    "paymentDate" -> jodaLocalDate(
      invalidKey = "error.date.hvd.real",
      allRequiredKey = "error.date.hvd.all",
      twoRequiredKey = "error.date.hvd.two",
      requiredKey = "error.date.hvd.one"
    ).verifying(
      jodaMinDate(CashPaymentFirstDateFormProvider.minDate, "error.date.hvd.past"),
      jodaMaxDate(CashPaymentFirstDateFormProvider.maxDate, "error.date.hvd.future")
    ).transform[CashPaymentFirstDate](
      CashPaymentFirstDate(_), _.paymentDate
    )
  )

}

object CashPaymentFirstDateFormProvider {

  val minDate: LocalDate = new LocalDate(1900, 1, 1)

  val maxDate: LocalDate = LocalDate.now()
}
