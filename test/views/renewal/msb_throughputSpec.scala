package views.renewal

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.MsbThroughput
import org.jsoup.nodes.Document
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture
import views.html.renewal.msb_total_throughput

class msb_throughputSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    override def view = msb_total_throughput(EmptyForm, edit = false)
  }

  trait InvalidFormFixture extends ViewFixture {

    val requiredMsg = Messages("renewal.msb.throughput.selection.required")

    val invalidForm = InvalidForm(
      Map.empty[String, Seq[String]],
      Seq(Path \ "throughput" -> Seq(ValidationError(requiredMsg)))
    )

    override def view = msb_total_throughput(invalidForm, edit = false)
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

      val getElement = (doc: Document) => doc.select(s"""input[type="radio"][name="throughput"][value="${selection.value}"]""")

      s"display the radio button for selection ${selection.value}" in new ViewFixture {
        Option(getElement(doc).first) mustBe defined
      }

      s"display the selection label for selection ${selection.value}" in new ViewFixture {
        val radioLabelElement = getElement(doc).first.parent

        Option(radioLabelElement) mustBe defined
        radioLabelElement.text mustBe Messages(selection.label)
      }
    }

    "display the 'save and continue' button" in new ViewFixture {
      doc.select("""button[type=submit][name=submit]""").text mustBe Messages("button.saveandcontinue")
    }

    "display the error summary" in new InvalidFormFixture {
      val summaryElement = doc.getElementsByClass("amls-error-summary").first
      Option(summaryElement) mustBe defined
      summaryElement.text must include(requiredMsg)
    }

    "display the validation error next to the field" in new InvalidFormFixture {
      val validationMsg = doc.select("#throughput .error-notification").first
      Option(validationMsg) mustBe defined
      validationMsg.text must include(requiredMsg)
    }
  }
}
