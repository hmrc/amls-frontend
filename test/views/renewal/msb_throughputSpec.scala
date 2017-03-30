package views.renewal

import forms.EmptyForm
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class msb_throughputSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    override def view = views.html.renewal.msb_total_throughput(EmptyForm)
  }

  "The MSB total throughput view" must {
    "display the correct header" in new ViewFixture {
      doc.select("header .heading-xlarge").text mustBe Messages("renewal.msb.throughput.header")
    }

    "display the correct secondary header" in new ViewFixture {
      doc.select("header .heading-secondary").text must include(Messages("summary.renewal"))
    }

    "display the correct title" in new ViewFixture {
      doc.title must include(s"${Messages("renewal.msb.throughput.header")} - ${Messages("summary.renewal")}")
    }

    "display the throughput selection radio buttons" in new ViewFixture {



    }

  }

}
