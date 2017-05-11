package views.confirmation

import models.confirmation.Currency
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class SubmitRegistrationConfirmationViewSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val continueHref = "http://google.co.uk"

    val fee = 100

    override def view = views.html.confirmation.confirmation_new("ref number", Currency(fee), Seq.empty, continueHref)
  }

  "The amendment confirmation view" must {

    "show the correct title" in new ViewFixture {
      doc.title must startWith(Messages("confirmation.header"))
    }

    "show the correct header" in new ViewFixture {
      doc.select(".heading-xlarge").text mustBe Messages("confirmation.header")
    }

    "show the correct secondary header" in new ViewFixture {
      doc.select(".heading-secondary").text must include(Messages("submit.registration"))
    }

    "show the correct informational paragraph" in new ViewFixture {
      doc.select(".panel-border-wide").text mustBe Messages("confirmation.submission.info")
    }

    "show the correct fee" in new ViewFixture {
      doc.select(".reg-online--pay-fee .heading-large").first.text mustBe s"£$fee.00"
    }

    "show the correct reference" in new ViewFixture {
      doc.select(".reg-online--pay-fee .heading-large").get(1).text mustBe "ref number"
    }

    "continue button has the right text" in new ViewFixture {
      doc.select(s".button[href=$continueHref]").text mustBe Messages("button.payfee")
    }

  }

}
