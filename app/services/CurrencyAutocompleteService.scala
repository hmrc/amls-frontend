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

package services

import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem

import javax.inject.Inject

class CurrencyAutocompleteService @Inject() () {

  lazy val formOptions: Seq[SelectItem] =
    SelectItem(None, "") +:
      models.currencies.map { currency =>
        SelectItem(
          value = Some(currency),
          text = currency.toUpperCase
        )
      }
}
