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

package models.businessactivities

import jto.validation.forms.UrlFormEncoded
import jto.validation._
import jto.validation.ValidationError
import play.api.libs.json._
import jto.validation.forms.Rules.{minLength => _, _}
import cats.data.Validated.{Invalid, Valid}
import utils.TraversableValidators.minLengthR

sealed trait RiskAssessmentPolicy

case class RiskAssessmentPolicyYes(riskassessments: Set[RiskAssessmentType]) extends RiskAssessmentPolicy

case object RiskAssessmentPolicyNo extends RiskAssessmentPolicy

sealed trait RiskAssessmentType

case object PaperBased extends RiskAssessmentType

case object Digital extends RiskAssessmentType

object RiskAssessmentType {

  import utils.MappingUtils.Implicits._

  implicit val riskAssessmentFormRead = Rule[String, RiskAssessmentType] {
    case "01" => Valid(PaperBased)
    case "02" => Valid(Digital)
    case _ =>
      Invalid(Seq((Path \ "riskassessments") -> Seq(ValidationError("error.invalid"))))
  }

  implicit val riskAssessmentFormWrite = Write[RiskAssessmentType, String] {
    case PaperBased => "01"
    case Digital => "02"
  }

  implicit val jsonRiskAssessmentReads: Reads[RiskAssessmentType] =
    Reads {
      case JsString("01") => JsSuccess(PaperBased)
      case JsString("02") => JsSuccess(Digital)
      case _ => JsError((JsPath \ "riskassessments") -> play.api.data.validation.ValidationError("error.invalid"))
    }

  implicit val jsonRiskAssessmentWrites =
    Writes[RiskAssessmentType] {
      case PaperBased => JsString("01")
      case Digital => JsString("02")
    }
}

object RiskAssessmentPolicy {

  import utils.MappingUtils.Implicits._

  implicit def formReads
  (implicit
   p: Path => RuleLike[UrlFormEncoded, Set[RiskAssessmentType]]
    ): Rule[UrlFormEncoded, RiskAssessmentPolicy] =
    From[UrlFormEncoded] { __ =>
      (__ \ "hasPolicy").read[Boolean].withMessage("error.required.ba.option.risk.assessment") flatMap {
          case true =>
             (__ \ "riskassessments").
               read(minLengthR[Set[RiskAssessmentType]](1).withMessage("error.required.ba.risk.assessment.format")) map RiskAssessmentPolicyYes.apply
         case false => Rule.fromMapping { _ => Valid(RiskAssessmentPolicyNo) }
      }

  }

  implicit def formWrites
  (implicit
   w: Write[RiskAssessmentType, String]
    ) = Write[RiskAssessmentPolicy, UrlFormEncoded] {
        case RiskAssessmentPolicyYes(data) =>
            Map("hasPolicy" -> Seq("true"),
            "riskassessments[]" -> data.toSeq.map(w.writes))
        case RiskAssessmentPolicyNo =>
            Map("hasPolicy" -> Seq("false"))

  }


  implicit def jsonReads: Reads[RiskAssessmentPolicy] =
    (__ \ "hasPolicy").read[Boolean] flatMap {
      case true =>
        (__ \ "riskassessments").read[Set[RiskAssessmentType]].flatMap(RiskAssessmentPolicyYes.apply _)
      case false => Reads(_ => JsSuccess(RiskAssessmentPolicyNo))
    }

  implicit def jsonWrites = Writes[RiskAssessmentPolicy] {
       case RiskAssessmentPolicyYes(data) =>
            Json.obj("hasPolicy" -> true,
            "riskassessments" -> data)
        case RiskAssessmentPolicyNo =>
            Json.obj("hasPolicy" -> false)
  }

}
