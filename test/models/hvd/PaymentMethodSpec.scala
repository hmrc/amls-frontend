package models.hvd

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.Success
import play.api.libs.json.JsSuccess

class PaymentMethodSpec extends PlaySpec {

  "PaymentMethod" must {

    "roundtrip through form" in {
      val data = PaymentMethod.Other("foo")
      PaymentMethod.formR.validate(PaymentMethod.formW.writes(data)) mustEqual Success(data)
    }

    "roundtrip through json" in {
      val data = PaymentMethod.Other("foo")
      PaymentMethod.jsonR.validate(PaymentMethod.jsonW.writes(data)) mustEqual Success(data)
    }
  }
}
