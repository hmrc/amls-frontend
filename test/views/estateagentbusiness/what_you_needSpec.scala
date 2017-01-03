package views.estateagentbusiness

import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import views.ViewFixture

class what_you_needSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "what_you_need view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.estateagentbusiness.what_you_need()

      doc.title must startWith(Messages("title.wyn") + " - " + Messages("summary.estateagentbusiness"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.estateagentbusiness.what_you_need()

      heading.html must be(Messages("title.wyn"))
      subHeading.html must include(Messages("summary.estateagentbusiness"))

    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.estateagentbusiness.what_you_need()

      html must include(Messages("estateagentbusiness.whatyouneed.subheading"))
      html must include(Messages("estateagentbusiness.whatyouneed.line_1"))
      html must include(Messages("estateagentbusiness.whatyouneed.line_2"))
      html must include(Messages("estateagentbusiness.whatyouneed.line_3"))
    }
  }
}
