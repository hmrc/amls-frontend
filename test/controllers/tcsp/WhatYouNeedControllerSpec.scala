package controllers.tcsp

import config.AMLSAuthConnector
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

class WhatYouNeedControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new WhatYouNeedController {
      override val authConnector = self.authConnector
    }
  }
  "WhatYouNeedController" must {

    "use correct services" in new Fixture {
      WhatYouNeedController.authConnector must be(AMLSAuthConnector)
    }

    "get" must {
      "load the page" in new Fixture {

        val pageTitle = Messages("title.wyn") + " - " +
          Messages("summary.tcsp") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(pageTitle)
      }
    }
  }
}
