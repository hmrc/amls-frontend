package views.bankdetails

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import  utils.GenericTestHelper
import play.api.i18n.Messages
import views.Fixture

class what_you_needSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = views.html.bankdetails.what_you_need(0)

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.bankdetails.what_you_need(0)

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.bankdetails"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.bankdetails.what_you_need(0)

      html must include(Messages("bankdetails.whatyouneed.line_1"))
      html must include(Messages("bankdetails.whatyouneed.line_2"))
      html must include(Messages("bankdetails.whatyouneed.line_3"))
    }
  }
}