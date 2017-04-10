package views.confirmation

import models.confirmation.Currency
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class AmendmentConfirmationViewSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val continueHref = "http://google.co.uk"

    override def view = views.html.confirmation.confirm_amendvariation("ref number", Currency(100), Seq.empty, Some(Currency(150)), continueHref)
  }

  "The amendment confirmation view" must {

    "show the correct title" in new ViewFixture {
      doc.title must startWith(Messages("confirmation.amendment.title"))
    }

    "show the correct header" in new ViewFixture {
      doc.select(".heading-xlarge").text mustBe Messages("confirmation.amendment.header")
    }

    "show the correct secondary header" in new ViewFixture {
      doc.select(".heading-secondary").text must include(Messages("confirmation.amendment.header.secondary"))
    }

    "show the correct informational paragraph" in new ViewFixture {
      doc.select(".info").text mustBe Messages("confirmation.amendment.info")
    }

    "show the correct fee" in new ViewFixture {
      doc.select(".reg-online--pay-fee .heading-large").first.text mustBe "£150.00"
    }

    "show the correct reference" in new ViewFixture {
      doc.select(".reg-online--pay-fee .heading-large").get(1).text mustBe "ref number"
    }

    "continue button has the right text" in new ViewFixture {
      doc.select(s".button[href=$continueHref]").text mustBe Messages("button.continue")
    }

  }

}
