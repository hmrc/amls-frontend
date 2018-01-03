/*
 * Copyright 2018 HM Revenue & Customs
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

package models.tradingpremises

import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import org.scalatest.{MustMatchers, WordSpec}
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError

class WhatDoesYourBusinessDoSpec extends WordSpec with MustMatchers{
  val model = WhatDoesYourBusinessDo(
    Set(
      BillPaymentServices,
      EstateAgentBusinessService,
      MoneyServiceBusiness))

  "WhatDoesYourBusinessDo" when {
    "input data is valid" must {
      "convert form data correctly" in {
        val formData = Map("activities[]" -> Seq("02", "03", "05"))
        WhatDoesYourBusinessDo.formRule.validate(formData) must be (Valid(model))
      }
    }

    "no items have been selected" must {
      "reject with a required message" in {
        val formData = Map("activities[]" -> Seq())
        WhatDoesYourBusinessDo.formRule.validate(formData) must be
        Invalid(List((Path \ "activities") -> List(ValidationError("error.required.tp.activity.your.business.do"))))
      }
    }
  }

  it must {
    "write correctly to a form" in {
      val formData = Map("activities[]" -> Seq("02", "03", "05"))
      WhatDoesYourBusinessDo.formWrite.writes(model) must be (formData)
    }
  }
}
