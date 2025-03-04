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

package models.responsiblepeople

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class ApprovalFlagsSpec extends PlaySpec {
  "ApprovalFlags" when {
    "all flags are defined" must {
      "be complete" in {
        val approvalFlags =
          ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(false))

        approvalFlags.isComplete() must be(true)
      }
    }

    "not all flags are defined" must {
      "no be complete" in {
        val approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = None)

        approvalFlags.isComplete() must be(false)
      }
    }
  }

  "ApprovalFlags Json" when {

    "there are both flags provided" must {

      "Read successfully" in {

        val json = Json.parse(
          """{
            | "hasAlreadyPassedFitAndProper": false,
            | "hasAlreadyPaidApprovalCheck": false
            |}""".stripMargin
        )

        val expected =
          ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false), hasAlreadyPaidApprovalCheck = Some(false))
        ApprovalFlags.format.reads(json) must be(
          JsSuccess(expected)
        )
      }

      "write successfully" in {

        val model = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false), hasAlreadyPaidApprovalCheck = Some(false))

        ApprovalFlags.format.writes(model) must be(
          Json.obj(
            "hasAlreadyPassedFitAndProper" -> false,
            "hasAlreadyPaidApprovalCheck"  -> false
          )
        )
      }
    }

    "there is only one flag provided" must {

      "Read successfully" in {
        val expected = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true))

        val json = Json.parse(
          """{
            | "hasAlreadyPassedFitAndProper": true
            |}""".stripMargin
        )

        ApprovalFlags.format.reads(json) must be(
          JsSuccess(expected)
        )
      }

      "Read successfully using designated reader" in {
        val expected = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true))

        val json = Json.parse(
          """{
            | "hasAlreadyPassedFitAndProper": true
            |}""".stripMargin
        )

        ApprovalFlags.reads.reads(json) must be(
          JsSuccess(expected)
        )
      }

      "write successfully" in {
        val expected = ApprovalFlags(hasAlreadyPaidApprovalCheck = Some(true))

        ApprovalFlags.format.writes(expected) must be(
          Json.obj(
            "hasAlreadyPaidApprovalCheck" -> true
          )
        )
      }

      "write successfully using designated writer" in {
        val expected = ApprovalFlags(hasAlreadyPaidApprovalCheck = Some(true))

        ApprovalFlags.writes.writes(expected) must be(
          Json.obj(
            "hasAlreadyPaidApprovalCheck" -> true
          )
        )
      }
    }
  }
}
