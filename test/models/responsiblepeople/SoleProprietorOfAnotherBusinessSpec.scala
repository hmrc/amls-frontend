package models.responsiblepeople

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{ValidationError, Path}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json, JsPath, JsSuccess}

class SoleProprietorOfAnotherBusinessSpec extends PlaySpec {

  "Form validation" must {
    "pass validation" when {
      "given a valid answer" in {

        val model = Map(
          "soleProprietorOfAnotherBusiness" -> Seq("true")
        )

        SoleProprietorOfAnotherBusiness.formRule.validate(model) must be(Valid(SoleProprietorOfAnotherBusiness(true)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty string" in {

        val model = Map(
          "soleProprietorOfAnotherBusiness" -> Seq("")
        )
        SoleProprietorOfAnotherBusiness.formRule.validate(model) must be(Invalid(Seq(
          Path \ "soleProprietorOfAnotherBusiness" -> Seq(ValidationError("error.required.rp.sole_proprietor", "personName"))
        )))
      }

      "given missing data represented by an empty Map" in {

        SoleProprietorOfAnotherBusiness.formRule.validate(Map.empty) must be(Invalid(Seq(
          Path \ "soleProprietorOfAnotherBusiness" -> Seq(ValidationError("error.required.rp.sole_proprietor", "personName"))
        )))
      }
    }

    "successfully write the model" in {

      SoleProprietorOfAnotherBusiness.formWrites.writes(SoleProprietorOfAnotherBusiness(true)) mustBe Map(
        "soleProprietorOfAnotherBusiness" -> Seq("true")
      )
    }
  }

  "Json validation" must {

    "Read and write successfully" in {

      SoleProprietorOfAnotherBusiness.format.reads(SoleProprietorOfAnotherBusiness.format.writes(SoleProprietorOfAnotherBusiness(true))) must be(
        JsSuccess(SoleProprietorOfAnotherBusiness(true), JsPath \ "soleProprietorOfAnotherBusiness"))
    }

    "write successfully" in {
      SoleProprietorOfAnotherBusiness.format.writes(SoleProprietorOfAnotherBusiness(true)) must be(Json.obj("soleProprietorOfAnotherBusiness" -> true))
    }
  }
}