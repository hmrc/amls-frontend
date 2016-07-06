package models.hvd

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.Success

class ReceiveCashPaymentsSpec extends PlaySpec {

  "ReceiveCashPayments" must {

    "roundtrip through form" in {
      val data = ReceiveCashPayments(Some(Set(PaymentMethod.Other("foo"))))
      ReceiveCashPayments.formR.validate(ReceiveCashPayments.formW.writes(data)) mustEqual Success(data)
    }

    "roundtrip through json" in {
      val data = ReceiveCashPayments(Some(Set(PaymentMethod.Other("foo"))))
      ReceiveCashPayments.jsonR.validate(ReceiveCashPayments.jsonW.writes(data)) mustEqual Success(data)

      println(ReceiveCashPayments.jsonW.writes(data))
    }
  }
}
