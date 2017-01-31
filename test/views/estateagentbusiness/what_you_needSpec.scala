package views.estateagentbusiness

import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import play.api.i18n.Messages
import views.Fixture

class what_you_needSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

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
