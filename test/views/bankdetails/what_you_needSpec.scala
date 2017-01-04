package views.bankdetails

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import views.ViewFixture

class what_you_needSpec extends WordSpec with MustMatchers with OneAppPerSuite{

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
      html must include(Messages("bankdetails.whatyouneed.line_4"))
      html must include(Messages("bankdetails.whatyouneed.line_5"))
    }
  }
}