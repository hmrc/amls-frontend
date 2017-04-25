package views.tradingpremises

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

      def view = views.html.tradingpremises.what_you_need(1, false)

      val title = Messages("title.wyn") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      doc.title must be(title)
      heading.html must be(Messages("title.wyn"))
      subHeading.html must include(Messages("summary.tradingpremises"))

    }

    "contain the expected content elements" in new ViewFixture {
      def view = views.html.tradingpremises.what_you_need(1, false)

      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.1"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.3"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.4"))

      doc.getElementsMatchingOwnText(Messages("button.continue")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("button.continue")).attr("href") must be("/anti-money-laundering/trading-premises/premises/1")
      doc.getElementsMatchingOwnText(Messages("main.sidebar.title")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("main.sidebar.information")).hasText must be(true)
    }

    "contain the expected content elements when mab is selected as one of the option in business activities" in new ViewFixture {
      def view = views.html.tradingpremises.what_you_need(1, true)


      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.1"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.3"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.4"))

      doc.getElementsMatchingOwnText(Messages("button.continue")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("main.sidebar.title")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("main.sidebar.information")).hasText must be(true)

      doc.getElementsMatchingOwnText(Messages("tradingpremises.whatyouneed.agents.sub.heading")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("tradingpremises.whatyouneed.agents.desc")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("button.continue")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("button.continue")).attr("href") must be("/anti-money-laundering/trading-premises/who-uses/1")
    }
  }
}
