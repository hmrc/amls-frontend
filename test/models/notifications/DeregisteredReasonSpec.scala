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

package models.notifications

import models.notifications.DeregisteredReason._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString

class DeregisteredReasonSpec extends PlaySpec {

  "DeregisteredReason model" must {
    "return reason for the string" in {
      DeregisteredReason.reason("01") must be(CeasedTrading)
      DeregisteredReason.reason("02") must be(HVDNoCashPayment)
      DeregisteredReason.reason("03") must be(OutOfScope)
      DeregisteredReason.reason("04") must be(NotTrading)
      DeregisteredReason.reason("05") must be(UnderAnotherSupervisor)
      DeregisteredReason.reason("06") must be(ChangeOfLegalEntity)
      DeregisteredReason.reason("99") must be(Other)
    }

    "write data successfully" in {
      DeregisteredReason.jsonWrites.writes(CeasedTrading) must be(JsString("01"))
      DeregisteredReason.jsonWrites.writes(HVDNoCashPayment) must be(JsString("02"))
      DeregisteredReason.jsonWrites.writes(OutOfScope) must be(JsString("03"))
      DeregisteredReason.jsonWrites.writes(NotTrading) must be(JsString("04"))
      DeregisteredReason.jsonWrites.writes(UnderAnotherSupervisor) must be(JsString("05"))
      DeregisteredReason.jsonWrites.writes(ChangeOfLegalEntity) must be(JsString("06"))
      DeregisteredReason.jsonWrites.writes(Other) must be(JsString("99"))
    }
  }
}
