package models.hvd

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{Json, JsSuccess}

class ReceiveCashPaymentsSpec extends PlaySpec {

  "ReceiveCashPayments" must {

    val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))

    "roundtrip through form" in {
      val data = ReceiveCashPayments(Some(paymentMethods))
      ReceiveCashPayments.formR.validate(ReceiveCashPayments.formW.writes(data)) mustEqual Success(data)
    }

    "roundtrip through json" in {
      val data = ReceiveCashPayments(Some(paymentMethods))
      ReceiveCashPayments.jsonR.reads(ReceiveCashPayments.jsonW.writes(data)) mustEqual JsSuccess(data)
    }

    "fail to validate when no choice is made for main question" in {
      val data = Map.empty[String, Seq[String]]
      ReceiveCashPayments.formR.validate(data)
        .mustEqual(Failure(Seq((Path \ "receivePayments") -> Seq(ValidationError("error.required.hvd.receive.cash.payments")))))
    }

    "fail to validate when no method is selected" in {
      val data = Map(
        "receivePayments" -> Seq("true")
      )
      ReceiveCashPayments.formR.validate(data)
        .mustEqual(Failure(Seq((Path \ "paymentMethods") -> Seq(ValidationError("error.required.hvd.choose.option")))))
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
