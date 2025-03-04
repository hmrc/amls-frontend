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

package models.businessmatching

import models.businessmatching.BusinessMatchingMsbService._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class BusinessMatchingMsbServicesSpec extends PlaySpec {

  "MsbServices" must {

    "round trip through Json correctly" in {

      val data = BusinessMatchingMsbServices(
        Set(TransmittingMoney, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange, ForeignExchange)
      )
      val js   = Json.toJson(data)
      js.as[BusinessMatchingMsbServices] mustEqual data
    }
  }
}
