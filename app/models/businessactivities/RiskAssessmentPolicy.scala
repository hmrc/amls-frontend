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

//sealed trait RiskAssessmentPolicy
//
//case class RiskAssessmentPolicyYes(riskassessments: Option[Set[RiskAssessmentType]]) extends RiskAssessmentPolicy
//
//case object RiskAssessmentPolicyNo extends RiskAssessmentPolicy
//
//sealed trait RiskAssessmentType
//
//case object PaperBased extends RiskAssessmentType
//
//case object Digital extends RiskAssessmentType
//
//object RiskAssessmentType {
//
//  implicit val riskAssessmentFormRead = Rule[String, RiskAssessmentType] {
//    case "01" => Valid(PaperBased)
//    case "02" => Valid(Digital)
//    case _ =>
//      Invalid(Seq((Path \ "riskassessments") -> Seq(ValidationError("error.invalid"))))
//  }
//
//  implicit val riskAssessmentFormWrite = Write[RiskAssessmentType, String] {
//    case PaperBased => "01"
//    case Digital => "02"
//  }
//
//  implicit val jsonRiskAssessmentReads: Reads[RiskAssessmentType] =
//    Reads {
//      case JsString("01") => JsSuccess(PaperBased)
//      case JsString("02") => JsSuccess(Digital)
//      case _ => JsError((JsPath \ "riskassessments") -> play.api.data.validation.ValidationError("error.invalid"))
//    }
//
//  implicit val jsonRiskAssessmentWrites =
//    Writes[RiskAssessmentType] {
//      case PaperBased => JsString("01")
//      case Digital => JsString("02")
//    }
//}
//
//object RiskAssessmentPolicy {
//
//  import utils.MappingUtils.Implicits._
//
//    type RiskAssessmentValidation = (Boolean, Option[Set[RiskAssessmentType]])
//
//    val validateRiskAssessmentType: ValidationRule[RiskAssessmentValidation] = Rule[RiskAssessmentValidation, RiskAssessmentValidation] {
//      case (a, x@None) => Valid(a, x)
//      case (b, x@Some(riskassessments) )if riskassessments.nonEmpty => Valid(b, x)
//      case _ => Invalid(Seq((Path \ "riskassessment") -> Seq(ValidationError("error.invalid.msb.wc.moneySources"))))
//    }
//
//  implicit def formRule(implicit  p: Path => Rule[UrlFormEncoded, Set[RiskAssessmentType]]):
//    Rule[UrlFormEncoded, RiskAssessmentPolicy] = From[UrlFormEncoded] { __ =>
//    import jto.validation.forms._
//
//    val riskassessmentTypes: Rule[UrlFormEncoded, Option[Set[RiskAssessmentType]]] =
//      (__ \ "riskassessments").read[Option[Set[RiskAssessmentType]]] map {
//        case _ => None
//      }
//
//    val hasPolicy: Rule[UrlFormEncoded, Boolean] =
//      (__ \ "hasPolicy").read[Boolean]
//
//    val validated = (hasPolicy ~ riskassessmentTypes).tupled andThen validateRiskAssessmentType
//
//    validated map { rav: RiskAssessmentValidation =>
//      (rav._1, rav._2) match {
//        case (true, b) => RiskAssessmentPolicyYes(b)
//        case (false, _) => RiskAssessmentPolicyNo
//      }
//
//    }
//  }
////
////  implicit def formReads
////  (implicit
////   p: Path => Rule[UrlFormEncoded, Option[Set[RiskAssessmentType]]]
////    ): Rule[UrlFormEncoded, RiskAssessmentPolicy] =
////    From[UrlFormEncoded] { __ =>
////      (__ \ "hasPolicy").read[Boolean].withMessage("error.required.ba.option.risk.assessment") flatMap {
//////          case true =>
//////             (__ \ "riskassessments").
//////               read(minLengthR[Set[RiskAssessmentType]](1).withMessage("error.required.ba.risk.assessment.format")) map RiskAssessmentPolicyYes.apply
////          case true =>
////             (__ \ "riskassessments").
////               read[Option[Set[RiskAssessmentType]]] map RiskAssessmentPolicyYes.apply
////         case false => Rule.fromMapping { _ => Valid(RiskAssessmentPolicyNo) }
////      }
////
////  }
//
//  implicit def formWrites
//  (implicit
//   w: Write[RiskAssessmentType, String]
//    ) = Write[RiskAssessmentPolicy, UrlFormEncoded] {
//        case RiskAssessmentPolicyYes(Some(data)) =>
//            Map("hasPolicy" -> Seq("true"),
//            "riskassessments[]" -> data.toSeq.map(w.writes))
//        case RiskAssessmentPolicyYes(None) =>
//            Map("hasPolicy" -> Seq("true"))
//        case RiskAssessmentPolicyNo =>
//            Map("hasPolicy" -> Seq("false"))
//
//  }
//
//
//  implicit def jsonReads: Reads[RiskAssessmentPolicy] =
//    (__ \ "hasPolicy").read[Boolean] flatMap {
//      case true =>
//        (__ \ "riskassessments").readNullable[Set[RiskAssessmentType]].flatMap(RiskAssessmentPolicyYes.apply)
//      case false => Reads(_ => JsSuccess(RiskAssessmentPolicyNo))
//    }
//
//  implicit def jsonWrites = Writes[RiskAssessmentPolicy] {
//       case RiskAssessmentPolicyYes(data) =>
//            Json.obj("hasPolicy" -> true,
//            "riskassessments" -> data)
//        case RiskAssessmentPolicyNo =>
//            Json.obj("hasPolicy" -> false)
//  }
//
//}

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

import play.api.libs.json.{Reads, Writes, _}


case class RiskAssessmentPolicy(hasPolicy: RiskAssessmentHasPolicy, riskassessments: RiskAssessmentTypes)

object RiskAssessmentPolicy {

  implicit val jsonReads: Reads[RiskAssessmentPolicy] = {
    import play.api.libs.functional.syntax._
    ((__ \ "hasPolicy").read[Boolean] and
      (__ \ "riskassessments").read[Set[RiskAssessmentType]]
      )((a, b) => RiskAssessmentPolicy(RiskAssessmentHasPolicy(a), RiskAssessmentTypes(b)) )
  }

  implicit val jsonWrites:Writes[RiskAssessmentPolicy] = {
    Writes[RiskAssessmentPolicy] {
      case RiskAssessmentPolicy(RiskAssessmentHasPolicy(true), RiskAssessmentTypes(a)) =>
        Json.obj(
          "hasPolicy" -> "true",
          "riskassessments" -> a.toSeq.map(d => d.toString)
        )
      case RiskAssessmentPolicy(RiskAssessmentHasPolicy(false), _) =>
        Json.obj(
          "hasPolicy" -> "false"
        )
    }
  }

//  def update(cashPayment: CashPayment, acceptedAnyPayment: CashPaymentOverTenThousandEuros): CashPayment = {
//    acceptedAnyPayment match {
//      case CashPaymentOverTenThousandEuros(false) => CashPayment(acceptedAnyPayment, None)
//      case CashPaymentOverTenThousandEuros(true) => CashPayment(acceptedAnyPayment, cashPayment.firstDate)
//    }
//  }
//
//  def update(cashPayment: CashPayment, firstDate: CashPaymentFirstDate): CashPayment =
//    CashPayment(CashPaymentOverTenThousandEuros(true), Some(firstDate))
}
