package controllers

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

class MainSummaryControllerSpec extends PlaySpec with OneServerPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new MainSummaryController {
      override val authConnector = self.authConnector
    }
  }

  "MainSummaryController" must {

    "load the main summary page" in new Fixture {
      val result = controller.onPageLoad(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("summary.title"))
    }

  }

}
