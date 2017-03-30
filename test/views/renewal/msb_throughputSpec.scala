package views.renewal

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class msb_throughputSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    override def view = views.html.renewal.msb_total_throughput()
  }

  "The MSB total throughput view" must {
    "display the correct header" in new ViewFixture {

      doc.select("header .heading-xlarge").text mustBe Messages("renewal.msb.throughput.header")

    }
  }

}
