package views.hvd

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import  utils.GenericTestHelper
import play.api.i18n.Messages
import views.ViewFixture

class what_you_needSpec extends GenericTestHelper with MustMatchers {

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = views.html.hvd.what_you_need()

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.hvd.what_you_need()

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.hvd"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.hvd.what_you_need()

      html must include(Messages("hvd.whatyouneed.line_1"))
      html must include(Messages("hvd.whatyouneed.line_2"))
      html must include(Messages("hvd.whatyouneed.line_3"))
      html must include(Messages("hvd.whatyouneed.line_4"))
      html must include(Messages("hvd.whatyouneed.line_5"))
      html must include(Messages("hvd.whatyouneed.line_6"))
      html must include(Messages("hvd.whatyouneed.line_7"))
    }
  }
}
