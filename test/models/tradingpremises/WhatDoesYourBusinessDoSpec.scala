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

package models.tradingpremises

import models.businessmatching.BusinessActivity.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class WhatDoesYourBusinessDoSpec extends AnyWordSpec with Matchers {
  val model = WhatDoesYourBusinessDo(Set(BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))

  "WhatDoesYourBusinessDo" must {

    "round trip through JSON" in {

      Json.toJson(model).as[WhatDoesYourBusinessDo] mustBe model
    }
  }
}
