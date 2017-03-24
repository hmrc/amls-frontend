package models.payments

import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.GenericTestHelper

class ReturnLocationSpec extends GenericTestHelper {

  "The ReturnLocation model" must {

    "correctly determine the absolute url based on the current request" when {

      "request is running on localhost" in {

        val call = controllers.routes.ConfirmationController.paymentConfirmation("reference")
        implicit val request = FakeRequest(GET, "http://localhost:9222/anti-money-laundering/confirmation")
        val model = ReturnLocation(call)

        model.returnUrl mustBe s"//localhost:9222${call.url}"

      }

      "request is running in some other environment" in {

        val call = controllers.routes.ConfirmationController.paymentConfirmation("reference")
        implicit val request = FakeRequest(GET, "https://www.qa-environment.fake/anti-money-laundering")
        val model = ReturnLocation(call)

        model.returnUrl mustBe call.url

      }

    }
  }
}
