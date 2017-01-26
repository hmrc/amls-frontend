package controllers.responsiblepeople

import config.AMLSAuthConnector
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

class WhoMustRegisterControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new WhoMustRegisterController {
      override val authConnector = self.authConnector
    }
  }
  "WhoMustRegisterController" must {

      "use correct services" in new Fixture {
        WhoMustRegisterController.authConnector must be(AMLSAuthConnector)
      }

    "get" must {

      "load the page" in new Fixture {
        val result = controller.get(1)(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("responsiblepeople.whomustregister.title"))
      }
    }
  }
}
