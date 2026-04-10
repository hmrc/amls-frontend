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

package models.governmentgateway

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class EnrolmentResponseSpec extends PlaySpec {

  "EnrolmentResponse" must {

    "serialise and deserialise correctly" in {
      val model = EnrolmentResponse(
        serviceName = "HMRC-MLR-ORG",
        state = "Activated",
        friendlyName = "AML Service",
        identifiersForDisplay = Seq(Identifier("MLRRefNumber", "XAML00000567890"))
      )
      Json.toJson(model).as[EnrolmentResponse] mustEqual model
    }

    "serialise and deserialise with empty identifiers" in {
      val model = EnrolmentResponse(
        serviceName = "HMRC-MLR-ORG",
        state = "Activated",
        friendlyName = "AML Service",
        identifiersForDisplay = Seq.empty
      )
      Json.toJson(model).as[EnrolmentResponse] mustEqual model
    }
  }

  "Identifier" must {

    "serialise and deserialise correctly" in {
      val model = Identifier("MLRRefNumber", "XAML00000567890")
      Json.toJson(model).as[Identifier] mustEqual model
    }
  }
}
