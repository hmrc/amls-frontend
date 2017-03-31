package views.renewal

import forms.EmptyForm
import models.renewal.MsbThroughput
import org.jsoup.nodes.Document
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

    "display the informational text" in new ViewFixture {
      doc.body().text must include(Messages("renewal.msb.throughput.info"))
    }

    MsbThroughput.throughputValues foreach { selection =>

      val getElement = (doc: Document) => doc.select(s"""input[type="radio"][name="throughputSelection"][value="${selection.code}"]""")

      s"display the radio button for selection ${selection.code}" in new ViewFixture {
        Option(getElement(doc).first) mustBe defined
      }

      s"display the selection label for selection ${selection.code}" in new ViewFixture {
        val radioLabelElement = getElement(doc).first.parent

        Option(radioLabelElement) mustBe defined
        radioLabelElement.text mustBe Messages(selection.label)
      }
    }

    "display the 'save and continue' button" in new ViewFixture {
      doc.select("""button[type=submit][name=submit]""").text mustBe Messages("button.saveandcontinue")
    }


  }

}
