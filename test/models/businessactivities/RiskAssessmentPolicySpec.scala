/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

class RiskAssessmentPolicySpec extends PlaySpec with MockitoSugar {

  val formalRiskAssessments: Set[RiskAssessmentType] = Set(PaperBased, Digital)

//  "RiskAssessment" must {

    import jto.validation.forms.Rules._

//    "fail validation" when {
//
//      "given invalid data represented by an empty map" in {
//        RiskAssessmentPolicy.formReads.validate(Map.empty) must
//          be(Invalid(Seq((Path \ "hasPolicy") -> Seq(ValidationError("error.required.ba.option.risk.assessment")))))
//      }
//
//      "given invalid enum value" in {
//        val model = Map(
//          "hasPolicy" -> Seq("true"),
//          "riskassessments[]" -> Seq("01", "99")
//        )
//
//        RiskAssessmentPolicy.formReads.validate(model) must
//          be(Invalid(Seq((Path \ "riskassessments" \ 1 \ "riskassessments", Seq(ValidationError("error.invalid"))))))
//      }
//
//      "given empty riskassessments data" in {
//        val model = Map(
//          "hasPolicy" -> Seq("true"),
//          "riskassessments[]" -> Seq()
//        )
//
//        RiskAssessmentPolicy.formRule.validate(model) must
//          be(Invalid(Seq((Path \ "riskassessments", Seq(ValidationError("error.required.ba.option.risk.assessment"))))))
//      }
//
//      "given missing hasPolicy data" in {
//        val model = Map(
//          "riskassessments[]" -> Seq("01", "02")
//        )
//
//        RiskAssessmentPolicy.formRule.validate(model) must
//          be(Invalid(Seq((Path \ "hasPolicy", Seq(ValidationError("error.required.ba.option.risk.assessment"))))))
//      }
//
//      "given missing hasPolicy data represented by an empty string" in {
//        val model = Map(
//          "hasPolicy" -> Seq(""),
//          "riskassessments[]" -> Seq("01", "02")
//        )
//
//        RiskAssessmentPolicy.formRule.validate(model) must
//          be(Invalid(Seq((Path \ "hasPolicy", Seq(ValidationError("error.required.ba.option.risk.assessment"))))))
//      }
//    }
//
//    "pass validation" when {
//      "successfully validate given an enum value" in {
//        RiskAssessmentPolicy.formRule.validate(Map("hasPolicy" -> Seq("false"))) must
//          be(Valid(RiskAssessmentPolicyNo))
//      }
//
//      "given `Yes` with no value" in {
//        val model = Map("hasPolicy" -> Seq("true"))
//        RiskAssessmentPolicy.formRule.validate(model) must
//          be(Valid(RiskAssessmentPolicyYes(None)))
//      }
//
//      "validate model with multiple check boxes selected" in {
//
//        val model = Map(
//          "hasPolicy" -> Seq("true"),
//          "riskassessments[]" -> Seq("01", "02")
//        )
//
//        RiskAssessmentPolicy.formRule.validate(model) must
//          be(Valid(RiskAssessmentPolicyYes(formalRiskAssessments)))
//      }
//    }
//
//    "write form data correctly" when {
//      "yes is selected and no risk assessment value is provided" in {
//        val model = Map(
//          "hasPolicy" -> Seq("true")
//        )
//
//        RiskAssessmentPolicy.formWrites.writes(RiskAssessmentPolicyYes(None)) must
//          be(model)
//      }
//
//      "yes is selected and risk assessment value is provided" in {
//        val model = Map(
//          "hasPolicy" -> Seq("true"),
//          "riskassessments[]" -> Seq("01", "02")
//        )
//
//        RiskAssessmentPolicy.formWrites.writes(RiskAssessmentPolicyYes(Some((Set(PaperBased, Digital)))) must
//          be(model)
//      }
//
//      "no is selected" in {
//        val model = Map(
//          "hasPolicy" -> Seq("false")
//        )
//
//        RiskAssessmentPolicy.formWrites.writes(RiskAssessmentPolicyNo) must
//          be(model)
//      }
//    }
//  }

  "JSON validation" must {
    "successfully validate" when {

      "hasPolicy is true and riskassesments field is populated" in {
        val json = Json.obj(
          "hasPolicy" -> true,
          "riskassessments" -> Seq("01", "02"))

        Json.fromJson[RiskAssessmentPolicy](json) must
          be(JsSuccess(RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(formalRiskAssessments)), JsPath))
      }

      "hasPolicy is false" in {
        val json = Json.obj("hasPolicy" -> false)

        Json.fromJson[RiskAssessmentPolicy](json) must
          be(JsSuccess(RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set())), JsPath))
      }
    }
    "fail validation" when {
      "given invalid data" in {
        Json.fromJson[RiskAssessmentPolicy](Json.obj("hasPolicy" -> "true", "riskassessments" -> Seq("01", "99"))) mustBe a[JsError]
      }
    }

    "successfully write JSON" when {
      "hasPolicy is true" in {
        Json.toJson[RiskAssessmentPolicy](RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(formalRiskAssessments))) must be(Json.obj("hasPolicy" -> "true",
          "riskassessments" -> Seq("01", "02")
        ))
      }

      "hasPolicy is false" in {
        Json.toJson[RiskAssessmentPolicy](RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), RiskAssessmentTypes(Set()))) must be(Json.obj("hasPolicy" -> "false"))
      }
    }
  }
}
