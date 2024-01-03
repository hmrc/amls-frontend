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

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class RiskAssessmentHasPolicySpec extends PlaySpec with MockitoSugar {

  val formalRiskAssessments: Set[RiskAssessmentType] = Set(PaperBased, Digital)

  "RiskAssessment" must {

    "fail validation" when {

      "given invalid data represented by an empty map" in {
        RiskAssessmentHasPolicy.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "hasPolicy") -> Seq(ValidationError("error.required.ba.option.risk.assessment")))))
      }

      "given missing hasPolicy data represented by an empty string" in {
        val model = Map(
          "hasPolicy" -> Seq("")
        )

        RiskAssessmentHasPolicy.formRule.validate(model) must
          be(Invalid(Seq((Path \ "hasPolicy", Seq(ValidationError("error.required.ba.option.risk.assessment"))))))
      }
    }

    "pass validation" when {
      "answering `Yes`" in {
        val model = Map("hasPolicy" -> Seq("true"))
        RiskAssessmentHasPolicy.formRule.validate(model) must
          be(Valid(RiskAssessmentHasPolicy(true)))
      }
      "answering `No`" in {
        val model = Map("hasPolicy" -> Seq("false"))
        RiskAssessmentHasPolicy.formRule.validate(model) must
          be(Valid(RiskAssessmentHasPolicy(false)))
      }
    }
    "write form data correctly" when {
      "yes is selected" in {
        val model = Map(
          "hasPolicy" -> Seq("true")
        )

        RiskAssessmentHasPolicy.formWrites.writes(RiskAssessmentHasPolicy(true)) must
          be(model)
      }

      "no is selected" in {
        val model = Map(
          "hasPolicy" -> Seq("false")
        )

        RiskAssessmentHasPolicy.formWrites.writes(RiskAssessmentHasPolicy(false)) must
          be(model)
      }
    }
  }
}