package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class MsbThroughputSpec extends PlaySpec with MustMatchers {

  "The MsbThroughput model" must {

    "be able to be serialized and deserialized" in {
      val model = MsbThroughput("01")

      Json.fromJson[MsbThroughput](Json.toJson(model)).asOpt mustBe Some(model)
    }

    "fail form validation" when {
      "nothing on the form is selected" in {

        val form = Map.empty[String, Seq[String]]

        MsbThroughput.formReader.validate(form) must be(
          Invalid(Seq(
            (Path \ "throughputSelection") -> Seq(ValidationError("renewal.msb.throughput.selection.required"))
          ))
        )

      }

      "an invalid value is sent" in {
        val form = Map(
          "throughputSelection" -> Seq("some invalid value")
        )

        MsbThroughput.formReader.validate(form) must be(
          Invalid(Seq(
            (Path \ "throughputSelection") -> Seq(ValidationError("renewal.msb.throughput.selection.invalid"))
          ))
        )
      }
    }

    "pass validation" when {
      "the correct value is passed through the form" in {
        val form = Map(
          "throughputSelection" -> Seq("01")
        )

        MsbThroughput.formReader.validate(form) must be(
          Valid(MsbThroughput("01"))
        )
      }
    }

    "write to the form correctly" in {
      val model = MsbThroughput("01")

      MsbThroughput.formWriter.writes(model) must be(
        Map("throughputSelection" -> Seq("01"))
      )
    }

  }
}
