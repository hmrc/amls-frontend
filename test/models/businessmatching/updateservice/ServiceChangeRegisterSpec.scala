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

package models.businessmatching.updateservice

import models.businessmatching._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class ServiceChangeRegisterSpec extends PlaySpec {

  "The Json serializer" must {
    "produce the correct Json" in {
      val model = ServiceChangeRegister(
        Some(Set(HighValueDealing, MoneyServiceBusiness)),
        Some(Set(TransmittingMoney))
      )

      Json.toJson(model) mustBe Json.obj(
        "addedActivities" -> Seq("04", "05"),
        "addedSubSectors" -> Seq("01")
      )
    }

    "be able to parse the json correctly" in {
      val json = Json.obj(
        "addedActivities" -> Seq("01", "02"),
        "addedSubSectors" -> Seq("02")
      )

      Json.fromJson[ServiceChangeRegister](json).get mustBe ServiceChangeRegister(
        Some(Set(AccountancyServices, BillPaymentServices)),
        Some(Set(CurrencyExchange)))
    }
  }

}
