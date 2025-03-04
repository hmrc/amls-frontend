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

package models.businessactivities

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class RiskAssessmentPolicySpec extends PlaySpec with MockitoSugar {

  val formalRiskAssessments: Set[RiskAssessmentType] = Set(PaperBased, Digital)

  "JSON validation" must {
    "successfully validate" when {

      "hasPolicy is true and riskassesments field is populated" in {
        val json = Json.obj("hasPolicy" -> true, "riskassessments" -> Seq("01", "02"))

        Json.fromJson[RiskAssessmentPolicy](json) must
          be(
            JsSuccess(
              RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(formalRiskAssessments)),
              JsPath
            )
          )
      }

      "hasPolicy is false" in {
        val json = Json.obj("hasPolicy" -> false)

        Json.fromJson[RiskAssessmentPolicy](json) must
          be(JsSuccess(RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set())), JsPath))
      }
    }
    "fail validation" when {
      "given invalid data" in {
        Json.fromJson[RiskAssessmentPolicy](
          Json.obj("hasPolicy" -> true, "riskassessments" -> Seq("01", "99"))
        ) mustBe a[JsError]
      }
    }

    "successfully write JSON" when {
      "hasPolicy is true" in {
        Json.toJson[RiskAssessmentPolicy](
          RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(formalRiskAssessments))
        ) must
          be(Json.obj("hasPolicy" -> true, "riskassessments" -> Seq("01", "02")))
      }

      "hasPolicy is false" in {
        Json.toJson[RiskAssessmentPolicy](
          RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set()))
        ) must
          be(Json.obj("hasPolicy" -> false))
      }
    }
  }
}
