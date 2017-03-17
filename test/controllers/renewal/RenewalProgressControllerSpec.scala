package controllers.renewal

import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import utils.{AuthorisedFixture, GenericTestHelper}

class RenewalProgressControllerSpec extends GenericTestHelper {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    lazy val app = new GuiceApplicationBuilder()
      .build()

    val controller = new RenewalProgressController(self.authConnector)

  }

  "The Renewal Progress Controller" must {

    "load the page" in new Fixture {

      val result = controller.get()(request)

      status(result) mustBe OK

      val html = Jsoup.parse(contentAsString(result))

      html.select(".page-header").text() must include(Messages("renewal.progress.title"))

    }

  }

}
