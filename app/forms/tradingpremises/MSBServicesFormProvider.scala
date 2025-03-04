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

package forms.tradingpremises

import forms.mappings.Mappings
import models.tradingpremises.{TradingPremisesMsbService, TradingPremisesMsbServices}
import play.api.data.Form
import play.api.data.Forms.seq

import javax.inject.Inject

class MSBServicesFormProvider @Inject() () extends Mappings {

  private val errorMessage = "error.required.tp.services"

  def apply(): Form[TradingPremisesMsbServices] = Form[TradingPremisesMsbServices](
    "value" -> seq(enumerable[TradingPremisesMsbService](errorMessage))
      .verifying(nonEmptySeq(errorMessage))
      .transform[TradingPremisesMsbServices](x => TradingPremisesMsbServices(x.toSet), _.services.toSeq)
  )
}
