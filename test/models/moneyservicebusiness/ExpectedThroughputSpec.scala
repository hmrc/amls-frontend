package models.moneyservicebusiness

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, Json, JsPath, JsSuccess}

class ExpectedThroughputSpec extends PlaySpec {

  "ExpectedThroughput" should {
    "Form Validation" must {

      "successfully validate given an enum value" in {

        ExpectedThroughput.formRule.validate(Map("throughput" -> Seq("01"))) must
          be(Valid(ExpectedThroughput.First))

        ExpectedThroughput.formRule.validate(Map("throughput" -> Seq("02"))) must
          be(Valid(ExpectedThroughput.Second))

        ExpectedThroughput.formRule.validate(Map("throughput" -> Seq("03"))) must
          be(Valid(ExpectedThroughput.Third))

        ExpectedThroughput.formRule.validate(Map("throughput" -> Seq("04"))) must
          be(Valid(ExpectedThroughput.Fourth))

        ExpectedThroughput.formRule.validate(Map("throughput" -> Seq("05"))) must
          be(Valid(ExpectedThroughput.Fifth))

        ExpectedThroughput.formRule.validate(Map("throughput" -> Seq("06"))) must
          be(Valid(ExpectedThroughput.Sixth))

        ExpectedThroughput.formRule.validate(Map("throughput" -> Seq("07"))) must
          be(Valid(ExpectedThroughput.Seventh))
      }

      "write correct data from enum value" in {

        ExpectedThroughput.formWrites.writes(ExpectedThroughput.First) must
          be(Map("throughput" -> Seq("01")))

        ExpectedThroughput.formWrites.writes(ExpectedThroughput.Second) must
          be(Map("throughput" -> Seq("02")))

        ExpectedThroughput.formWrites.writes(ExpectedThroughput.Third) must
          be(Map("throughput" -> Seq("03")))

        ExpectedThroughput.formWrites.writes(ExpectedThroughput.Fourth) must
          be(Map("throughput" -> Seq("04")))

        ExpectedThroughput.formWrites.writes(ExpectedThroughput.Fifth) must
          be(Map("throughput" -> Seq("05")))

        ExpectedThroughput.formWrites.writes(ExpectedThroughput.Sixth) must
          be(Map("throughput" -> Seq("06")))

        ExpectedThroughput.formWrites.writes(ExpectedThroughput.Seventh) must
          be(Map("throughput" -> Seq("07")))
      }


      "throw error on invalid data" in {
        ExpectedThroughput.formRule.validate(Map("throughput" -> Seq("20"))) must
          be(Invalid(Seq((Path \ "throughput", Seq(ValidationError("error.invalid"))))))
      }

      "throw error on empty data" in {
        ExpectedThroughput.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "throughput", Seq(ValidationError("error.required.msb.throughput"))))))
      }
    }

    "JSON validation" must {

      "successfully validate given an enum value" in {

        Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "01")) must
          be(JsSuccess(ExpectedThroughput.First, JsPath))

        Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "02")) must
          be(JsSuccess(ExpectedThroughput.Second, JsPath))

        Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "03")) must
          be(JsSuccess(ExpectedThroughput.Third, JsPath))

        Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "04")) must
          be(JsSuccess(ExpectedThroughput.Fourth, JsPath))

        Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "05")) must
          be(JsSuccess(ExpectedThroughput.Fifth, JsPath))

        Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "06")) must
          be(JsSuccess(ExpectedThroughput.Sixth, JsPath))

        Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "07")) must
          be(JsSuccess(ExpectedThroughput.Seventh, JsPath))
      }

      "write the correct value" in {

        Json.toJson(ExpectedThroughput.First) must
          be(Json.obj("throughput" -> "01"))

        Json.toJson(ExpectedThroughput.Second) must
          be(Json.obj("throughput" -> "02"))

        Json.toJson(ExpectedThroughput.Third) must
          be(Json.obj("throughput" -> "03"))

        Json.toJson(ExpectedThroughput.Fourth) must
          be(Json.obj("throughput" -> "04"))

        Json.toJson(ExpectedThroughput.Fifth) must
          be(Json.obj("throughput" -> "05"))

        Json.toJson(ExpectedThroughput.Sixth) must
          be(Json.obj("throughput" -> "06"))

        Json.toJson(ExpectedThroughput.Seventh) must
          be(Json.obj("throughput" -> "07"))
      }

      "throw error for invalid data" in {
        Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "20")) must
          be(JsError(JsPath, play.api.data.validation.ValidationError("error.invalid")))
      }
    }
   
  }
}
