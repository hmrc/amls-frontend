package models.businessmatching

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class BusinessAppliedForPSRNumberSpec extends PlaySpec {

  "BusinessAppliedForPSRNumber" should {

    "Form Validation" must {

      "successfully read form data" when {
        "given the option yes with a valid psr number" in {

          val map = Map("appliedFor" -> Seq("true"),
            "regNumber" -> Seq("123789"))

          BusinessAppliedForPSRNumber.formRule.validate(map) must be(Valid(BusinessAppliedForPSRNumberYes("123789")))
        }

        "given the option no" in {

          val map = Map("appliedFor" -> Seq("false"))

          BusinessAppliedForPSRNumber.formRule.validate(map) must be(Valid(BusinessAppliedForPSRNumberNo))
        }

        "given letters, numbers, spaces and hyphens" in {

          val regNumber = "67-s G"

          val form = Map(
            "appliedFor" -> Seq("true"),
            "regNumber" -> Seq(regNumber)
          )

          BusinessAppliedForPSRNumber.formRule.validate(form) must be(Valid(BusinessAppliedForPSRNumberYes(regNumber)))

        }
      }

      "fail validation" when {
        "given missing data represented by an empty Map" in {

          BusinessAppliedForPSRNumber.formRule.validate(Map.empty) must be(Invalid(Seq((Path \ "appliedFor",
            Seq(ValidationError("error.required.msb.psr.options"))))))
        }

        "given a 'yes' value with a missing psr number respresented by an empty string" in {
          val map = Map("appliedFor" -> Seq("true"),
            "regNumber" -> Seq(""))

          BusinessAppliedForPSRNumber.formRule.validate(map) must be(Invalid(Seq((Path \ "regNumber",
            Seq(ValidationError("error.invalid.msb.psr.number"))))))
        }

        "given a 'yes' value with a missing psr number respresented by a missing field" in {
          val map = Map("appliedFor" -> Seq("true"))

          BusinessAppliedForPSRNumber.formRule.validate(map) must be(Invalid(Seq((Path \ "regNumber",
            Seq(ValidationError("error.required"))))))
        }

        "given a 'yes' value with an invalid psr number with too many characters" in {
          val map = Map("appliedFor" -> Seq("true"),
            "regNumber" -> Seq("a" * 7))

          BusinessAppliedForPSRNumber.formRule.validate(map) must be(Invalid(Seq((Path \ "regNumber",
            Seq(ValidationError("error.invalid.msb.psr.number"))))))
        }

        "given a 'yes' value with invalid characters in the PSR number" in {
          val form = Map(
            "appliedFor" -> Seq("true"),
            "regNumber" -> Seq("$£%0")
          )

          BusinessAppliedForPSRNumber.formRule.validate(form) must be(
            Invalid(Seq(Path \ "regNumber" -> Seq(ValidationError("error.invalid.msb.psr.number"))))
          )
        }
      }

      "Successfully write form data" in {
        BusinessAppliedForPSRNumber.formWrites.writes(BusinessAppliedForPSRNumberNo) must be(Map("appliedFor" -> Seq("false")))
      }
    }

    "Json Validation" must {

      "Successfully read and write data:option yes" in {
        BusinessAppliedForPSRNumber.jsonReads.reads(BusinessAppliedForPSRNumber.jsonWrites.writes(BusinessAppliedForPSRNumberYes("123456"))) must
          be(JsSuccess(BusinessAppliedForPSRNumberYes("123456"), JsPath \ "regNumber"))
      }

      "Successfully read and write data:option No" in {
        BusinessAppliedForPSRNumber.jsonReads.reads(BusinessAppliedForPSRNumber.jsonWrites.writes(BusinessAppliedForPSRNumberNo)) must
          be(JsSuccess(BusinessAppliedForPSRNumberNo, JsPath))
      }
    }
  }
}

