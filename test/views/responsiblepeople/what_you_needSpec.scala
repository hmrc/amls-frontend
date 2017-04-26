package views.responsiblepeople

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
      def view = views.html.responsiblepeople.what_you_need(1)

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.responsiblepeople.what_you_need(1)

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.responsiblepeople"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.responsiblepeople.what_you_need(1)

      html must include(Messages("responsiblepeople.whatyouneed.requiredinfo"))

      html must include(Messages("responsiblepeople.whatyouneed.possiblerequiredinfo"))

      html must include(Messages("responsiblepeople.whatyouneed.line_1"))
      html must include(Messages("responsiblepeople.whatyouneed.line_2"))
      html must include(Messages("responsiblepeople.whatyouneed.line_3"))
      html must include(Messages("responsiblepeople.whatyouneed.line_4"))
      html must include(Messages("responsiblepeople.whatyouneed.line_5"))
    }
  }
}
