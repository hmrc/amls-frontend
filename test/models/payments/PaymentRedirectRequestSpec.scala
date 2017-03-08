package models.payments

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class PaymentRedirectRequestSpec extends PlaySpec {

  "The PaymentRedirectRequest type" must {

    "serialize to the correct JSON format" in {

      val expectedJson = Json.obj(
        "reference" -> "some_reference",
        "amount" -> "100.0",
        "url" -> "http://google.co.uk"
      )

      val model = PaymentRedirectRequest("some_reference", 100, "http://google.co.uk")

      Json.toJson(model) mustBe expectedJson

    }

  }

}
