package views.asp

import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import views.ViewFixture

class what_you_needSpec extends WordSpec with MustMatchers with OneAppPerSuite{

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = views.html.asp.what_you_need()

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.asp.what_you_need()

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.asp"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.asp.what_you_need()

      html must include(Messages("asp.whatyouneed.line_1"))
      html must include(Messages("asp.whatyouneed.line_2"))
      html must include(Messages("asp.whatyouneed.line_3"))
    }
  }
}
