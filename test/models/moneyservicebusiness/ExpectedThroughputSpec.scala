package models.moneyservicebusiness

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class ExpectedThroughputSpec extends PlaySpec {

  "ExpectedThroughput" should {

    "Form" must {

      "successfully Read Form data" in {

        val map =  Map("throughput" -> Seq("01"))
        ExpectedThroughput.formRule.validate(map) must be(Success(ExpectedThroughput("01")))
      }

      "throw error for  missing form data" in {
        ExpectedThroughput.formRule.validate(Map.empty) must be(Failure(Seq(
          (Path \ "throughput") -> Seq(ValidationError("error.required.msb.throughput"))
        )))
      }

      "successfully write data" in {
        ExpectedThroughput.formWrites.writes(ExpectedThroughput("02")) must be(Map("throughput" -> Seq("02")))
      }
    }

    "Json" must {

      "successfully read and write data" in {

        ExpectedThroughput.format.reads(ExpectedThroughput.format.writes(ExpectedThroughput("02"))) must be(
          JsSuccess(ExpectedThroughput("02"), JsPath \ "throughput"))
      }
    }
  }
}
