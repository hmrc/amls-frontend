package controllers.tcsp

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import utils.{AuthorisedFixture, GenericTestHelper}

class WhatYouNeedControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val controller = new WhatYouNeedController {
      override val authConnector = self.authConnector
    }
  }

  "WhatYouNeedController" when {

    "get is called" must {

      "respond with SEE_OTHER and redirect to the 'what you need' page" in new Fixture {

        val result = controller.get()(request)
        status(result) must be(OK)
      }
    }
  }
}
