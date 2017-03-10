package models.tradingpremises

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}


class IsResidentialSpec extends PlaySpec {
  // scalastyle:off

  "Form validation" must {
    "pass validation" when {
      "given a valid answer" in {

        val model = Map(
          "isResidential" -> Seq("true")
        )

        IsResidential.formRule.validate(model) must be(Valid(IsResidential(true)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty string" in {

        val model = Map(
          "isResidential" -> Seq("")
        )
        IsResidential.formRule.validate(model) must be(Invalid(Seq(
          Path \ "isResidential" -> Seq(ValidationError("tradingpremises.yourtradingpremises.isresidential.required"))
        )))
      }

      "given missing data represented by an empty Map" in {

        IsResidential.formRule.validate(Map.empty) must be(Invalid(Seq(
          Path \ "isResidential" -> Seq(ValidationError("tradingpremises.yourtradingpremises.isresidential.required"))
        )))
      }
    }

    "successfully write the model" in {

      IsResidential.formWrites.writes(IsResidential(true)) mustBe Map(
        "isResidential" -> Seq("true")
      )
    }
  }

  "Json validation" must {

    "Read and write successfully" in {

      IsResidential.format.reads(IsResidential.format.writes(IsResidential(true))) must be(
        JsSuccess(IsResidential(true), JsPath \ "isResidential"))
    }

    "write successfully" in {
      IsResidential.format.writes(IsResidential(true)) must be(Json.obj("isResidential" -> true))
    }
  }
}
