package controllers

import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

class StatusControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar{

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new StatusController {
      override val authConnector = self.authConnector
    }
  }

  "StatusController" should {

    "load the status page" in new Fixture{

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.title() must be(Messages("status.title"))
      document.getElementsByClass("heading-xlarge").first().child(1).html() must be(Messages("status.heading"))
      document.getElementsByClass("heading-secondary").first().html() must be(Messages("summary.status"))
      document.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

    }
  }

}
