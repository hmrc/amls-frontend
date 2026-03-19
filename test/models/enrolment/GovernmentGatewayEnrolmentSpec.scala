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

package models.enrolment

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class GovernmentGatewayEnrolmentSpec extends PlaySpec {

  "EnrolmentIdentifier" must {

    "serialise and deserialise correctly" in {
      val model = EnrolmentIdentifier("MLRRefNumber", "XAML00000567890")
      Json.toJson(model).as[EnrolmentIdentifier] mustEqual model
    }
  }

  "GovernmentGatewayEnrolment" must {

    "serialise and deserialise correctly with identifiers" in {
      val model = GovernmentGatewayEnrolment(
        key         = "HMRC-MLR-ORG",
        identifiers = List(EnrolmentIdentifier("MLRRefNumber", "XAML00000567890")),
        state       = "Activated"
      )
      Json.toJson(model).as[GovernmentGatewayEnrolment] mustEqual model
    }

    "serialise and deserialise correctly with empty identifiers" in {
      val model = GovernmentGatewayEnrolment(
        key         = "HMRC-MLR-ORG",
        identifiers = List.empty,
        state       = "NotYetActivated"
      )
      Json.toJson(model).as[GovernmentGatewayEnrolment] mustEqual model
    }
  }
}