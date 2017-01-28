package models.hvd

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, Json, JsSuccess}

class PaymentMethodSpec extends PlaySpec {

  "PaymentMethod" must {

    "roundtrip through form" in {
      val data = PaymentMethods(courier = true, direct = true, other = Some("foo"))
      PaymentMethods.formR.validate(PaymentMethods.formW.writes(data)) mustEqual Valid(data)
    }

    "roundtrip through json" in {
      val data = PaymentMethods(courier = true, direct = true, other = Some("foo"))
      val js = Json.toJson(data)
      js.as[PaymentMethods] mustEqual data
    }

    "fail to validate when no payment method is selected" in {
      val data = Map.empty[String, Seq[String]]
      PaymentMethods.formR.validate(data) mustEqual Invalid(Seq(Path -> Seq(ValidationError("error.required.hvd.choose.option"))))
    }

    "fail to validate when other is selected without details" in {
      val data = Map(
        "other" -> Seq("true"),
        "details" -> Seq("")
      )
      PaymentMethods.formR.validate(data) mustEqual Invalid(Seq((Path \ "details") -> Seq(ValidationError("error.required.hvd.describe"))))
    }
  }
}
