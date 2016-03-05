package models.businessactivities

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.data.mapping.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLength

sealed trait RiskAssessmentPolicy

case class RiskAssessmentPolicyYes(riskassessments: Set[RiskAssessmentType]) extends RiskAssessmentPolicy

case object RiskAssessmentPolicyNo extends RiskAssessmentPolicy

sealed trait RiskAssessmentType

case object PaperBased extends RiskAssessmentType

case object Digital extends RiskAssessmentType

object RiskAssessmentType {

  import utils.MappingUtils.Implicits._

  implicit val riskAssessmentFormRead = Rule[String, RiskAssessmentType] {
    case "01" => Success(PaperBased)
    case "02" => Success(Digital)
    case _ =>
      Failure(Seq((Path \ "riskassessments") -> Seq(ValidationError("error.invalid"))))
  }

  implicit val riskAssessmentFormWrite = Write[RiskAssessmentType, String] {
    case PaperBased => "01"
    case Digital => "02"
  }

  implicit val jsonRiskAssessmentReads: Reads[RiskAssessmentType] =
    Reads {
      case JsString("01") => JsSuccess(PaperBased)
      case JsString("02") => JsSuccess(Digital)
      case _ => JsError((JsPath \ "riskassessments") -> ValidationError("error.invalid"))
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
      (__ \ "hasPolicy").read[Boolean] flatMap {
          case true =>
             (__ \ "riskassessments").read(minLength[Set[RiskAssessmentType]](1)) fmap RiskAssessmentPolicyYes.apply
         case false => Rule.fromMapping { _ => Success(RiskAssessmentPolicyNo) }
      }

  }

  implicit def formWrites
  (implicit
   w: Write[RiskAssessmentType, String]
    ) = Write[RiskAssessmentPolicy, UrlFormEncoded] {
        case RiskAssessmentPolicyYes(data) =>
            Map("hasPolicy" -> Seq("true"),
            "riskassessments" -> data.toSeq.map(w.writes))
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
