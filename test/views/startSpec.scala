package views

import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import play.api.i18n.Messages

class startSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "Landing Page View" must {

    "Have the correct title" in new ViewFixture {
      def view = views.html.start()
      doc.title must startWith(Messages("start.title"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.start()

      heading.html must be (Messages("start.title"))

    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.start()

      html must include(Messages("start.line1"))
      html must include(Messages("start.line2"))
      html must include(Messages("start.line3"))
      html must include(Messages("start.before.heading"))
      html must include(Messages("start.utr.line1"))
      html must include(Messages("start.before.line1"))

    }
  }
}