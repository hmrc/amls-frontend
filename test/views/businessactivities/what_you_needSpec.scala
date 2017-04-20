package views.businessactivities

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class what_you_needSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = views.html.businessactivities.what_you_need()

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.businessactivities.what_you_need()

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.businessactivities"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.businessactivities.what_you_need()

      html must include(Messages("businessactivities.whatyouneed.line_1"))
      html must include(Messages("businessactivities.whatyouneed.line_2"))
      html must include(Messages("businessactivities.whatyouneed.line_3"))
    }
  }
}