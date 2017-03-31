package views.confirmation

import models.confirmation.Currency
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class VariationConfirmationViewSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val continueHref = "http://google.co.uk"

    override def view = views.html.confirmation.confirmation_variation("ref number", Currency(100), Seq.empty, continueHref)
  }

  "The variation confirmation view" must {

    "show the correct header" in new ViewFixture {
       doc.select("header.page-header .heading-xlarge").text mustBe Messages("confirmation.variation.header")
    }

    "show the correct subheading" in new ViewFixture {
      doc.select("header.page-header .heading-secondary").text must include(Messages("confirmation.variation.header.secondary"))
    }

    "show the correct information text" in new ViewFixture {
      doc.select(".panel-indent").first.text mustBe Messages("confirmation.variation.info")
    }

    "show the correct fee" in new ViewFixture {
      doc.select(".reg-online--pay-fee .heading-large").first.text mustBe "£100.00"
    }

    "show the correct reference" in new ViewFixture {
      doc.select(".reg-online--pay-fee .heading-large").get(1).text mustBe "ref number"
    }

    "continue button has the right text" in new ViewFixture {
      doc.select(s".button[href=$continueHref]").text mustBe Messages("button.continue")
    }

  }

}
