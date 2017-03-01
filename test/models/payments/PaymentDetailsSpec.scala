package models.payments

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class PaymentDetailsSpec extends PlaySpec {

  "The payments form" must {

    "round-trip through Json serialization properly" in {

      val model = PaymentDetails("a reference number", 5.50f)

      Json.fromJson[PaymentDetails](Json.toJson(model)) mustBe JsSuccess(model)

    }

  }
}
