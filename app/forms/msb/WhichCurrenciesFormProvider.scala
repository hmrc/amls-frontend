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

import forms.mappings.Mappings
import models.moneyservicebusiness.WhichCurrencies
import play.api.data.Form
import play.api.data.Forms.{optional, seq}
import play.api.data.validation.{Constraint, Invalid, Valid}

import javax.inject.Inject

class WhichCurrenciesFormProvider @Inject() () extends Mappings {

  private val emptyErrorKey = "error.invalid.msb.wc.currencies"

  def apply(): Form[WhichCurrencies] = Form[WhichCurrencies](
    "currencies" -> seq(
      optional(
        text(emptyErrorKey).verifying(isCurrencyConstraint)
      )
    ).verifying(nonEmptyOptionalSeq(emptyErrorKey))
      .transform[WhichCurrencies](x => WhichCurrencies(x.distinct.flatten), _.currencies.distinct.map(Some(_)))
  )

  private val isCurrencyConstraint: Constraint[String] = Constraint {
    case str if models.currencies.contains(str) =>
      Valid
    case _                                      =>
      Invalid(s"$emptyErrorKey.format")
  }
}
