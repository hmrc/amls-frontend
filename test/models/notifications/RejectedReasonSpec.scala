/*
 * Copyright 2017 HM Revenue & Customs
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

import models.notifications.RejectedReason._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString

class RejectedReasonSpec extends PlaySpec {

  "RejectedReason model" must {
    "return reason for the string" in {
      RejectedReason.reason("01") must be(NonCompliant)
      RejectedReason.reason("02") must be(FailedToRespond)
      RejectedReason.reason("03") must be(FailedToPayCharges)
      RejectedReason.reason("04") must be(FitAndProperFailure)
      RejectedReason.reason("98") must be(OtherFailed)
      RejectedReason.reason("99") must be(OtherRefused)
    }

    "write data successfully" in {
      RejectedReason.jsonWrites.writes(NonCompliant) must be(JsString("01"))
      RejectedReason.jsonWrites.writes(FailedToRespond) must be(JsString("02"))
      RejectedReason.jsonWrites.writes(FailedToPayCharges) must be(JsString("03"))
      RejectedReason.jsonWrites.writes(FitAndProperFailure) must be(JsString("04"))
      RejectedReason.jsonWrites.writes(OtherFailed) must be(JsString("98"))
      RejectedReason.jsonWrites.writes(OtherRefused) must be(JsString("99"))
    }
  }
}
