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

import org.scalacheck.Gen
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class AgentRemovalReasonSpec extends PlaySpec with ScalaCheckDrivenPropertyChecks {

  import models.tradingpremises.RemovalReasonConstants._

  "The AgentRemovalReason model" when {

    "given a valid model" must {

      "return the correct json" in {
        val model = AgentRemovalReason("Other", Some("Some other reason"))

        Json.toJson(model) must be(
          Json.obj(
            "removalReason"      -> Schema.OTHER,
            "removalReasonOther" -> "Some other reason"
          )
        )
      }

    }

    "given valid json" must {

      val json =
        """
          | {
          | "removalReason": "Other",
          | "removalReasonOther": "Some reason"
          | }
        """.stripMargin

      "return the model" in {

        Json.parse(json).asOpt[AgentRemovalReason] must be(Some(AgentRemovalReason("Other", Some("Some reason"))))

      }

    }

    "correctly parse form reason to schema reason and vice versa" in {
      val formReason = Gen.oneOf(
        Form.MAJOR_COMPLIANCE_ISSUES,
        Form.MINOR_COMPLIANCE_ISSUES,
        Form.LACK_OF_PROFIT,
        Form.CEASED_TRADING,
        Form.REQUESTED_BY_AGENT,
        Form.OTHER
      )

      forAll(formReason) { reason =>
        Rules.fromSchemaReason(Rules.toSchemaReason(reason)) mustBe reason
      }
    }

  }

}
