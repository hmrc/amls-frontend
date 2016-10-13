package models.businessmatching

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class BusinessAppliedForPSRNumberSpec extends PlaySpec {

  "BusinessAppliedForPSRNumber" should {

    "Form Validation" must {

      "Successfully read form data for the option yes" in {

        val map = Map("appliedFor" -> Seq("true"),
        "regNumber" -> Seq("123789"))

        BusinessAppliedForPSRNumber.formRule.validate(map) must be(Success(BusinessAppliedForPSRNumberYes("123789")))
      }

      "Successfully read form data for the option no" in {

        val map = Map("appliedFor" -> Seq("false"))

        BusinessAppliedForPSRNumber.formRule.validate(map) must be(Success(BusinessAppliedForPSRNumberNo))
      }

      "fail validation when user has not selected the radio button" in {

        BusinessAppliedForPSRNumber.formRule.validate(Map.empty) must be(Failure(Seq((Path \ "appliedFor",
          Seq(ValidationError("error.required.msb.psr.options"))))))
      }

      "fail validation when user has not filled PSR registration number" in {
        val map = Map("appliedFor" -> Seq("true"),
          "regNumber" -> Seq(""))

        BusinessAppliedForPSRNumber.formRule.validate(map) must be(Failure(Seq((Path \ "regNumber",
          Seq(ValidationError("error.invalid.msb.psr.number"))))))
      }

      "Successfully write form data" in {
        BusinessAppliedForPSRNumber.formWrites.writes(BusinessAppliedForPSRNumberNo) must be(Map("appliedFor" -> Seq("false")))
      }
    }

    "Json Validation" must {

      "Successfully read and write data:option yes" in {
        BusinessAppliedForPSRNumber.jsonReads.reads(BusinessAppliedForPSRNumber.jsonWrites.writes(BusinessAppliedForPSRNumberYes("123456"))) must
          be(JsSuccess(BusinessAppliedForPSRNumberYes("123456"), JsPath \ "appliedFor" \ "regNumber"))
      }

      "Successfully read and write data:option No" in {
        BusinessAppliedForPSRNumber.jsonReads.reads(BusinessAppliedForPSRNumber.jsonWrites.writes(BusinessAppliedForPSRNumberNo)) must
          be(JsSuccess(BusinessAppliedForPSRNumberNo, JsPath \ "appliedFor"))
      }
    }
  }
}

