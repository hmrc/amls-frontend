package views.hvd

import forms.{InvalidForm, ValidForm, Form2}
import models.hvd.PercentageOfCashPaymentOver15000
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class percentageSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "percentage view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[PercentageOfCashPaymentOver15000] = Form2(PercentageOfCashPaymentOver15000())

      def view = views.html.hvd.percentage(form2, true)

      doc.title must startWith("ExpectedTitleTextHere")
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[PercentageOfCashPaymentOver15000] = Form2(PercentageOfCashPaymentOver15000()

      def view = views.html.hvd.percentage(form2, true)

      heading.html must be(Messages("expectedHeadingText"))
      subHeading.html must include(Messages("ExpectedSubHeading"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "blah") -> Seq(ValidationError("not a message Key")),
          (Path \ "blah2") -> Seq(ValidationError("second not a message Key")),
          (Path \ "blah3") -> Seq(ValidationError("third not a message Key"))
        ))

      def view = views.html.hvd.receiving(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")

      doc.getElementById("id1").html() must include("not a message Key")
      doc.getElementById("id2").html() must include("second not a message Key")
      doc.getElementById("id3").html() must include("third not a message Key")
    }
  }
}
