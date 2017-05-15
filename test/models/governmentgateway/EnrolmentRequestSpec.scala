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

package models.governmentgateway

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class EnrolmentRequestSpec extends PlaySpec {

  "EnrolmentRequest" must {

    "serialise correctly" in {

      val model = EnrolmentRequest("foo", "bar")
      val json = Json.obj(
          "portalId" -> "Default",
          "serviceName" -> "HMRC-MLR-ORG",
          "friendlyName" -> "AMLS Enrolment",
          "knownFacts" -> Seq(
            "foo",
            "",
            "",
            "bar"
          )
        )

      Json.toJson(model) must
        equal (json)
    }
  }
}
