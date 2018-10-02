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

package models.responsiblepeople

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class ApprovalFlagsSpec extends PlaySpec {

  trait Fixture {
    val approvalFlagsModel = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false), hasAlreadyPaidApprovalCheck = Some(false))
  }

  "ApprovalFlags Json" when {

    "there are both flags provided" must {

      "Read successfully" in new Fixture {

        val json = Json.parse(
          """{
            | "hasAlreadyPassedFitAndProper": false,
            | "hasAlreadyPaidApprovalCheck": false
            |}""".stripMargin
        )

        ApprovalFlags.format.reads(json) must be(
          JsSuccess(approvalFlagsModel)
        )
      }

      "write successfully" in new Fixture {

        ApprovalFlags.format.writes(approvalFlagsModel) must be (
          Json.obj(
            "hasAlreadyPassedFitAndProper" -> false,
            "hasAlreadyPaidApprovalCheck" -> false
          )
        )
      }
    }

    "there is only one flag provided" must {

      "Read successfully" in new Fixture {
        override val approvalFlagsModel = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true))

        val json = Json.parse(
          """{
            | "hasAlreadyPassedFitAndProper": true
            |}""".stripMargin
        )

        ApprovalFlags.format.reads(json) must be(
          JsSuccess(approvalFlagsModel)
        )
      }

      "write successfully" in new Fixture {
        override val approvalFlagsModel = ApprovalFlags(hasAlreadyPaidApprovalCheck = Some(true))

        ApprovalFlags.format.writes(approvalFlagsModel) must be (
          Json.obj(
            "hasAlreadyPaidApprovalCheck" -> true
          )
        )
      }
    }
  }
}
