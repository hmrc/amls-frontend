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

package models.moneyservicebusiness

import models.Country
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class SendTheLargestAmountsOfMoneySpec extends PlaySpec {

  "SendTheLargestAmountsOfMoney" must {

    "roundtrip through json" in {

      val model: SendTheLargestAmountsOfMoney =
        SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))

      Json.fromJson[SendTheLargestAmountsOfMoney](Json.toJson(model)) mustEqual JsSuccess(model)
    }

    "correctly parse the json if country_1 and country_3 fields provided" in {
      val json     = Json.obj("country_1" -> "GB", "country_3" -> "IN")
      val expected = SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB"), Country("India", "IN")))

      Json.fromJson[SendTheLargestAmountsOfMoney](json) mustEqual JsSuccess(expected)
    }
  }
}
