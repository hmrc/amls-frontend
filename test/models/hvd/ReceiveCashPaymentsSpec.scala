package models.hvd

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{Json, JsSuccess}

class ReceiveCashPaymentsSpec extends PlaySpec {

  "ReceiveCashPayments" must {

    val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))

    "roundtrip through form" in {
      val data = ReceiveCashPayments(Some(paymentMethods))
      ReceiveCashPayments.formR.validate(ReceiveCashPayments.formW.writes(data)) mustEqual Valid(data)
    }

    "roundtrip through json" in {
      val data = ReceiveCashPayments(Some(paymentMethods))
      ReceiveCashPayments.jsonR.reads(ReceiveCashPayments.jsonW.writes(data)) mustEqual JsSuccess(data)
    }

    "fail to validate when no choice is made for main question" in {
      val data = Map.empty[String, Seq[String]]
      ReceiveCashPayments.formR.validate(data)
        .mustEqual(Invalid(Seq((Path \ "receivePayments") -> Seq(ValidationError("error.required.hvd.receive.cash.payments")))))
    }

    "fail to validate when no method is selected" in {
      val data = Map(
        "receivePayments" -> Seq("true")
      )
      ReceiveCashPayments.formR.validate(data)
        .mustEqual(Invalid(Seq((Path \ "paymentMethods") -> Seq(ValidationError("error.required.hvd.choose.option")))))
    }

    "fail to validate when no text is entered in the details field" in {
      val data = Map(
        "receivePayments" -> Seq("true"),
        "paymentMethods.other" -> Seq("true")
      )

      ReceiveCashPayments.formR.validate(data)
        .mustEqual(Invalid(Seq((Path \ "paymentMethods" \ "details") -> Seq(ValidationError("error.required")))))
    }

    "fail to validate when more than 255 characters are entered in the details field" in {
      val data = Map(
        "receivePayments" -> Seq("true"),
        "paymentMethods.other" -> Seq("true"),
        "paymentMethods.details" -> Seq("a" * 260)
      )

      ReceiveCashPayments.formR.validate(data)
        .mustEqual(Invalid(Seq((Path \ "paymentMethods" \ "details") -> Seq(ValidationError("error.invalid.maxlength.255")))))
    }
  }

  "RecieveCashPayments Serialisation" when {
    "paymentsMethods is empty" must {
      "set the receivePayments to false and the payment methods to an empty object" in {
        val res = Json.toJson(ReceiveCashPayments(None))
        res mustBe Json.obj(
          "receivePayments" -> false,
          "paymentMethods" -> Json.obj()
        )
      }
    }
  }
}
