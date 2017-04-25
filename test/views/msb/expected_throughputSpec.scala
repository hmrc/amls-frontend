package views.msb

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.moneyservicebusiness.ExpectedThroughput
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class expected_throughputSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "expected_throughput view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ExpectedThroughput] = Form2(ExpectedThroughput.First)

      def view = views.html.msb.expected_throughput(form2, true)

      doc.title must be(Messages("msb.throughput.title") +
        " - " + Messages("summary.msb") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ExpectedThroughput] = Form2(ExpectedThroughput.First)

      def view = views.html.msb.expected_throughput(form2, true)

      heading.html must be(Messages("msb.throughput.title"))
      subHeading.html must include(Messages("summary.msb"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "throughput") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.msb.expected_throughput(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("throughput")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}