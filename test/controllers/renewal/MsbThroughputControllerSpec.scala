package controllers.renewal

import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, GenericTestHelper}

class MsbThroughputControllerSpec extends GenericTestHelper {

  trait Fixture extends AuthorisedFixture { self =>
    implicit val request = addToken(authRequest)

    lazy val controller = new MsbThroughputController(
      self.authConnector
    )
  }

  "The MSB throughput controller" must {
    "return the view" in new Fixture {

      val result = controller.get()(request)

      status(result) mustBe OK

      contentAsString(result) must include(Messages("renewal.msb.throughput.header"))

    }

    "return a bad request result when an invalid form is posted" in new Fixture {
      val result = controller.post()(request)

      status(result) mustBe BAD_REQUEST
    }

  }

}
