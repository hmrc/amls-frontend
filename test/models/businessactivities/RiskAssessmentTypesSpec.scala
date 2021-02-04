/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class RiskAssessmentTypesSpec extends PlaySpec with MockitoSugar {

  val formalRiskAssessments: Set[RiskAssessmentType] = Set(PaperBased, Digital)

  "RiskAssessment" must {

    "fail validation" when {
      "given invalid data represented by an empty string" in {
        RiskAssessmentType.riskAssessmentFormRead.validate("") must
          be(Invalid(Seq((Path \ "riskassessments") -> Seq(ValidationError("error.invalid")))))
      }

      "given invalid enum value" in {
        RiskAssessmentType.riskAssessmentFormRead.validate("99") must
          be(Invalid(Seq((Path \ "riskassessments") -> Seq(ValidationError("error.invalid")))))
      }
    }

    "pass validation" when {
      "successfully validate given an enum value" in {
        RiskAssessmentType.riskAssessmentFormRead.validate("01") must
          be(Valid(PaperBased))
      }
    }
    "write form data correctly" when {
      "selecting paper based" in {
        RiskAssessmentType.jsonRiskAssessmentWrites.writes(PaperBased) must be(JsString("01"))
      }
      "selecting digitally" in {
        RiskAssessmentType.jsonRiskAssessmentWrites.writes(Digital) must be(JsString("02"))
      }
    }
  }
}