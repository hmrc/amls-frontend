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

import forms.mappings.CountryListMappings
import models.moneyservicebusiness.MostTransactions
import play.api.data.Form

import javax.inject.Inject

class MostTransactionsFormProvider @Inject()() extends CountryListMappings {

  private val emptyErrorKey = "error.required.countries.msb.most.transactions"

  def apply(): Form[MostTransactions] = Form[MostTransactions](
    "mostTransactionsCountries" -> countryListMapping[MostTransactions](emptyErrorKey)(
      x => MostTransactions(x.flatten.distinct), _.countries.distinct.map(Some(_))
    )
  )

  override val countryErrorKey: String = ""
}
