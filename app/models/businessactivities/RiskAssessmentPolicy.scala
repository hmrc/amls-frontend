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

import play.api.libs.json.{Reads, Writes, _}

case class RiskAssessmentPolicy(hasPolicy: RiskAssessmentHasPolicy, riskassessments: RiskAssessmentTypes)

object RiskAssessmentPolicy {

  implicit val jsonReads: Reads[RiskAssessmentPolicy] = {
    import play.api.libs.functional.syntax._
    ((__ \ "hasPolicy").read[Boolean] and
      (__ \ "riskassessments").readNullable[Set[RiskAssessmentType]])((hasPolicy, riskAssessmentTypes) =>
      RiskAssessmentPolicy(
        RiskAssessmentHasPolicy(hasPolicy),
        RiskAssessmentTypes(riskAssessmentTypes.getOrElse(Set[RiskAssessmentType]()))
      )
    )
  }

  implicit val jsonWrites: Writes[RiskAssessmentPolicy] =
    Writes[RiskAssessmentPolicy] {
      case RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(riskAssessmentTypes)) =>
        Json.obj(
          "hasPolicy"       -> true,
          "riskassessments" -> riskAssessmentTypes.toSeq.map(_.value)
        )

      case RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), _) =>
        Json.obj(
          "hasPolicy" -> false
        )
    }
}
