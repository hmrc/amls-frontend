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

import cats.data.Validated.{Invalid, Valid}
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.libs.json._

sealed trait RiskAssessmentType1

case object PaperBased1 extends RiskAssessmentType1

case object Digital1 extends RiskAssessmentType1

object RiskAssessmentType1 {

  implicit val riskAssessmentFormRead = Rule[String, RiskAssessmentType1] {
    case "01" => Valid(PaperBased1)
    case "02" => Valid(Digital1)
    case _ =>
      Invalid(Seq((Path \ "riskassessments") -> Seq(ValidationError("error.invalid"))))
  }

  implicit val riskAssessmentFormWrite = Write[RiskAssessmentType1, String] {
    case PaperBased1 => "01"
    case Digital1 => "02"
  }

  implicit val jsonRiskAssessmentReads: Reads[RiskAssessmentType1] =
    Reads {
      case JsString("01") => JsSuccess(PaperBased1)
      case JsString("02") => JsSuccess(Digital1)
      case _ => JsError((JsPath \ "riskassessments") -> play.api.data.validation.ValidationError("error.invalid"))
    }

  implicit val jsonRiskAssessmentWrites =
    Writes[RiskAssessmentType1] {
      case PaperBased1 => JsString("01")
      case Digital1 => JsString("02")
    }
}

case class RiskAssessmentTypes(riskassessments: Set[RiskAssessmentType1])

object RiskAssessmentTypes {

  implicit def formRule(implicit p: Path => Rule[UrlFormEncoded, Set[RiskAssessmentType1]]):
  Rule[UrlFormEncoded, RiskAssessmentTypes] = From[UrlFormEncoded] { __ =>
    (__ \ "riskassessments").read[Set[RiskAssessmentType1]] map RiskAssessmentTypes.apply
  }

  implicit def formWrites
  (implicit
   w: Write[RiskAssessmentType1, String]
  ) = Write[RiskAssessmentTypes, UrlFormEncoded] { data =>
    Map("riskassessments[]" -> data.riskassessments.toSeq.map(w.writes))
  }
}