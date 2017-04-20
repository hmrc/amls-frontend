package views.tcsp

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class what_you_needSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "what_you_need view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.tcsp.what_you_need()

      val title = Messages("title.wyn") + " - " +
        Messages("summary.tcsp") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      doc.title must be(title)
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.tcsp.what_you_need()

      heading.html must be(Messages("title.wyn"))
      subHeading.html must include(Messages("summary.tcsp"))

    }

    "contain the expected content elements" in new ViewFixture {
      def view = views.html.tcsp.what_you_need()

      html must include(Messages("tcsp.whatyouneed.requiredinfo.text.1"))
      html must include(Messages("tcsp.whatyouneed.requiredinfo.text.2"))
      html must include(Messages("tcsp.whatyouneed.requiredinfo.text.3"))
    }
  }
}