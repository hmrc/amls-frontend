package views.responsiblepeople

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class who_must_registerSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "who_must_register View" must {
    "Have the correct title" in new ViewFixture {
      def view = views.html.responsiblepeople.who_must_register(1, false)

      doc.title must be(Messages("responsiblepeople.whomustregister.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.responsiblepeople.who_must_register(1, false)

      heading.html must be (Messages("responsiblepeople.whomustregister.title"))
      subHeading.html must include (Messages("summary.responsiblepeople"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.responsiblepeople.who_must_register(1, false)


      html must include(Messages("responsiblepeople.whomustregister.line_1"))
      html must include(Messages("responsiblepeople.whomustregister.line_2"))
      html must include(Messages("responsiblepeople.whomustregister.line_3"))

      html must include(Messages("main.sidebar.title"))
      html must include(Messages("main.sidebar.information"))
    }
  }
}
