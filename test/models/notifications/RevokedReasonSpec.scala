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

import models.notifications.RevokedReason._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString

class RevokedReasonSpec extends PlaySpec {

  "RevokedReason model" must {
    "return reason for the string" in {
      RevokedReason.reason("01") must be(RevokedMissingTrader)
      RevokedReason.reason("02") must be(RevokedCeasedTrading)
      RevokedReason.reason("03") must be(RevokedNonCompliant)
      RevokedReason.reason("04") must be(RevokedFitAndProperFailure)
      RevokedReason.reason("05") must be(RevokedFailedToPayCharges)
      RevokedReason.reason("06") must be(RevokedFailedToRespond)
      RevokedReason.reason("99") must be(RevokedOther)
    }

    "write data successfully" in {
      RevokedReason.jsonWrites.writes(RevokedMissingTrader) must be(JsString("01"))
      RevokedReason.jsonWrites.writes(RevokedCeasedTrading) must be(JsString("02"))
      RevokedReason.jsonWrites.writes(RevokedNonCompliant) must be(JsString("03"))
      RevokedReason.jsonWrites.writes(RevokedFitAndProperFailure) must be(JsString("04"))
      RevokedReason.jsonWrites.writes(RevokedFailedToPayCharges) must be(JsString("05"))
      RevokedReason.jsonWrites.writes(RevokedFailedToRespond) must be(JsString("06"))
      RevokedReason.jsonWrites.writes(RevokedOther) must be(JsString("99"))
    }
  }
}
