package views.renewal

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture
import views.html.renewal._

class transactions_in_last_12_monthsSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    override def view = transactions_in_last_12_months(EmptyForm, edit = false)
  }

  trait InvalidFormFixture extends ViewFixture {

    val requiredMsg = Messages("renewal.msb.transfers.invalid")

    val invalidForm = InvalidForm(
      Map.empty[String, Seq[String]],
      Seq(Path \ "transfers" -> Seq(ValidationError(requiredMsg)))
    )

    override def view = transactions_in_last_12_months(invalidForm, edit = false)
  }

  "The MSB money transfers view" must {
    "display the correct header" in new ViewFixture {
      doc.select("header .heading-xlarge").text mustBe Messages("renewal.msb.transfers.header")
    }

    "display the correct secondary header" in new ViewFixture {
      doc.select("header .heading-secondary").text must include(Messages("summary.renewal"))
    }

    "display the correct title" in new ViewFixture {
      doc.title must include(s"${Messages("renewal.msb.transfers.header")} - ${Messages("summary.renewal")}")
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
      val validationMsg = doc.select("label[for=transfers] .error-notification").first
      Option(validationMsg) mustBe defined
      validationMsg.text must include(requiredMsg)
    }
  }
}
