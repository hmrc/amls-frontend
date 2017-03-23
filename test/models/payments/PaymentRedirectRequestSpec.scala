package models.payments

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class PaymentRedirectRequestSpec extends PlaySpec {

  "The PaymentRedirectRequest type" must {

    "serialize to the correct JSON format" in {

      implicit val request = FakeRequest(GET, "http://localhost:9222/anti-money-laundering")

      val expectedJson = Json.obj(
        "reference" -> "some_reference",
        "amount" -> "100.0",
        "url" -> "//localhost:9222/anti-money-laundering/start"
      )

      val model = PaymentRedirectRequest("some_reference", 100, ReturnLocation("/anti-money-laundering/start"))

      Json.toJson(model) mustBe expectedJson

    }

  }

}
