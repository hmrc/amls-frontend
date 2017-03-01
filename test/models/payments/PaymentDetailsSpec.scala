package models.payments

import models.confirmation.Currency
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class PaymentDetailsSpec extends PlaySpec {

  "The payments form" must {

    "round-trip through Json serialization properly" in {

      val model = PaymentDetails("a reference number", 120)

      Json.fromJson[PaymentDetails](Json.toJson(model)) mustBe JsSuccess(model)

    }

  }
}
