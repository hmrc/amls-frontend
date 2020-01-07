/*
 * Copyright 2020 HM Revenue & Customs
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

import cats.data.Validated.{Invalid, Valid}
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import utils.TraversableValidators._

sealed trait RiskAssessmentType

case object PaperBased extends RiskAssessmentType

case object Digital extends RiskAssessmentType

object RiskAssessmentType {

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

case class RiskAssessmentTypes(riskassessments: Set[RiskAssessmentType])

object RiskAssessmentTypes {
  import utils.MappingUtils.Implicits._

  implicit def formRule(implicit p: Path => Rule[UrlFormEncoded, Set[RiskAssessmentType]]):
  Rule[UrlFormEncoded, RiskAssessmentTypes] = From[UrlFormEncoded] { __ =>
    (__ \ "riskassessments").read(minLengthR[Set[RiskAssessmentType]](1)).withMessage("error.required.ba.risk.assessment.format") map RiskAssessmentTypes.apply
  }

  implicit def formWrites
  (implicit
   w: Write[RiskAssessmentType, String]
  ) = Write[RiskAssessmentTypes, UrlFormEncoded] { data =>
    Map("riskassessments[]" -> data.riskassessments.toSeq.map(w.writes))
  }
}