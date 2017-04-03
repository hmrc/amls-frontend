package controllers.renewal

import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, GenericTestHelper}

class MsbMoneyTransferControllerSpec extends GenericTestHelper {

  trait Fixture extends AuthorisedFixture { self =>
    val request = addToken(authRequest)

    lazy val controller = new MsbMoneyTransfersController(self.authConnector)
  }

  "Calling the GET action" must {
    "return the correct view" in new Fixture {
      val result = controller.get()(request)

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".heading-xlarge").text mustBe Messages("renewal.msb.transfers.header")
    }
  }

}
