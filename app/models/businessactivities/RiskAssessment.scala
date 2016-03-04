package models.businessactivities

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import utils.TraversableValidators._

case class RiskAssessments(riskassessmets: Set[RiskAssessment])

sealed trait RiskAssessment

case object PaperBased extends RiskAssessment

case object Digital extends RiskAssessment

object RiskAssessment {
  import utils.MappingUtils.Implicits._

  implicit val riskAssessmentFormRead = Rule[String, RiskAssessment] {
    case "01" => Success(PaperBased)
    case "02" => Success(Digital)
    case _ =>
      Failure(Seq((Path \ "riskassessments") -> Seq(ValidationError("error.invalid"))))
  }

  implicit val riskAssessmentFormWrite = Write[RiskAssessment, String] {
    case PaperBased => "01"
    case Digital => "02"
  }

  implicit val jsonRiskAssessmentReads: Reads[RiskAssessment] =
    Reads {
      case JsString("01") => JsSuccess(PaperBased)
      case JsString("02") => JsSuccess(Digital)
      case _ => JsError((JsPath \ "riskassessments") -> ValidationError("error.invalid"))
    }

  implicit val jsonRiskAssessmentWrites =
    Writes[RiskAssessment] {
      case PaperBased => JsString("01")
      case Digital => JsString("02")
    }
}

object RiskAssessments {

  import utils.MappingUtils.Implicits._

  implicit def formReads
  (implicit
   p: Path => RuleLike[UrlFormEncoded, Set[RiskAssessment]]
    ): Rule[UrlFormEncoded, RiskAssessments] =
    From[UrlFormEncoded] { __ =>
      val data = (__ \ "riskassessments").read(minLength[Set[RiskAssessment]](1))
      data flatMap(f =>
       if(f.seq.isEmpty){
          (Path \ "riskassessments") -> Seq(ValidationError("error.required"))
        } else {
          data fmap RiskAssessments.apply
      })
  }

  implicit def formWrites
  (implicit
   w: Write[RiskAssessment, String]
    ) = Write[RiskAssessments, UrlFormEncoded] { data =>
    Map("riskassessments" -> data.riskassessmets.toSeq.map(w.writes))
  }

  implicit val formats = Json.format[RiskAssessments]

}
