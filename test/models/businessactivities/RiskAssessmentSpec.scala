package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class RiskAssessmentSpec extends PlaySpec with MockitoSugar {

  "RiskAssessmentSpec" must {

    import play.api.data.mapping.forms.Rules._

    val formalRiskAssessments: Set[RiskAssessment] = Set(PaperBased, Digital)

    "validate model with few check box selected" in {

      val model = Map(
        "riskassessments[]" -> Seq("01", "02")
      )

      RiskAssessments.formReads.validate(model) must
        be(Success(RiskAssessments(formalRiskAssessments)))

    }

    "fail to validate on empty Map" in {

      RiskAssessments.formReads.validate(Map.empty) must
        be(Failure(Seq((Path \ "riskassessments") -> Seq(ValidationError("error.required")))))

    }

    "fail to validate when given invalid data" in {
      val model = Map(
        "riskassessments[]" -> Seq("01", "99")
      )

      RiskAssessments.formReads.validate(model) must
        be(Failure(Seq((Path \ "riskassessments" \ 1 \ "riskassessments", Seq(ValidationError("error.invalid"))))))
    }

    "write correct data for services value" in {

      RiskAssessments.formWrites.writes(RiskAssessments(Set(PaperBased,Digital))) must
        be(Map("riskassessments" -> Seq("01", "02")))

    }

    "JSON validation" must {

      "successfully validate given values" in {
        val json =  Json.obj("riskassessments" -> Seq("01","02"))

        Json.fromJson[RiskAssessments](json) must
          be(JsSuccess(RiskAssessments(formalRiskAssessments), JsPath \ "riskassessments"))
      }

    }

  }

}
