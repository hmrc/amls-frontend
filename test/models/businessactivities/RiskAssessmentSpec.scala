package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class RiskAssessmentSpec extends PlaySpec with MockitoSugar {

  "RiskAssessmentSpec" must {

    import play.api.data.mapping.forms.Rules._

    val formalRiskAssessments: Set[RiskAssessmentType] = Set(PaperBased, Digital)

    "fail to validate on empty map" in {
      RiskAssessmentPolicy.formReads.validate(Map.empty) must
        be(Failure(Seq((Path \ "hasPolicy") -> Seq(ValidationError("error.required")))))
    }

    "successfully validate given an enum value" in {
      RiskAssessmentPolicy.formReads.validate(Map("hasPolicy" -> Seq("false"))) must
        be(Success(RiskAssessmentPolicyNo))
    }


    "validate model with few check box selected" in {

      val model = Map(
        "hasPolicy" -> Seq("true"),
        "riskassessments[]" -> Seq("01", "02")
      )

      RiskAssessmentPolicy.formReads.validate(model) must
        be(Success(RiskAssessmentPolicyYes(formalRiskAssessments)))

    }

    "fail to validate given `Yes` with no value" in {

      val model = Map(
        "hasPolicy" -> Seq("true")
      )

      RiskAssessmentPolicy.formReads.validate(model) must
        be(Failure(Seq(
          (Path \ "riskassessments") -> Seq(ValidationError("error.required"))
        )))

    }

    "fail to validate when given invalid data" in {
      val model = Map(
        "hasPolicy" -> Seq("true"),
        "riskassessments[]" -> Seq("01", "99")
      )

      RiskAssessmentPolicy.formReads.validate(model) must
        be(Failure(Seq((Path \ "riskassessments" \ 1 \ "riskassessments", Seq(ValidationError("error.invalid"))))))
    }


    "write correct data for risk assessment value" in {

      val model = Map(
        "hasPolicy" -> Seq("true"),
        "riskassessments" -> Seq("01", "02")
      )

      RiskAssessmentPolicy.formWrites.writes(RiskAssessmentPolicyYes(Set(PaperBased, Digital))) must
        be(model)

    }

    "JSON validation" must {

      "successfully validate given values" in {
        val json = Json.obj(
          "hasPolicy" -> true,
          "riskassessments" -> Seq("01", "02"))

        Json.fromJson[RiskAssessmentPolicy](json) must
          be(JsSuccess(RiskAssessmentPolicyYes(formalRiskAssessments), JsPath \ "hasPolicy" \ "riskassessments"))
      }

      "successfully validate given values with option No" in {
        val json = Json.obj("hasPolicy" -> false)

        Json.fromJson[RiskAssessmentPolicy](json) must
          be(JsSuccess(RiskAssessmentPolicyNo, JsPath \ "hasPolicy"))
      }

      "fail when on invalid data" in {
        Json.fromJson[RiskAssessmentPolicy](Json.obj("hasPolicy" -> true,"riskassessments" -> Seq("01","03"))) mustBe a[JsError]
      }

      "write valid data in using json write" in {
        Json.toJson[RiskAssessmentPolicy](RiskAssessmentPolicyYes(Set(PaperBased, Digital))) must be(Json.obj("hasPolicy" -> true,
          "riskassessments" -> Seq("01", "02")
        ))
      }

      "write valid data in using json write with Option No" in {
        Json.toJson[RiskAssessmentPolicy](RiskAssessmentPolicyNo) must be(Json.obj("hasPolicy" -> false))
      }

    }
  }
}
