package views.msb

import forms.{Form2, ValidForm}
import models.moneyservicebusiness.WhichCurrencies
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.api.test.FakeApplication
import utils.GenericTestHelper
import views.Fixture


class which_currenciesSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> true))

  "which_currencies view" must {
    "have correct title" in new ViewFixture {

      val formData: ValidForm[WhichCurrencies] = Form2(WhichCurrencies(Seq("GBP"), None, None, None, None))

      def view = views.html.msb.which_currencies(formData, true)

      doc.title must startWith(Messages("msb.which_currencies.title") + " - " + Messages("summary.msb"))
    }

    "have correct headings" in new ViewFixture {

      val formData: ValidForm[WhichCurrencies] = Form2(WhichCurrencies(Seq("GBP"), None, None, None, None))

      def view = views.html.msb.which_currencies(formData, true)

      heading.html must be(Messages("msb.which_currencies.title"))
      subHeading.html must include(Messages("summary.msb"))

    }

    "ask the user whether they deal in foreign currencies" in new ViewFixture {

      val formData: ValidForm[WhichCurrencies] = Form2(WhichCurrencies(Seq("GBP"), None, None, None, None))

      def view = views.html.msb.which_currencies(formData, true)

      Option(doc.getElementById("usesForeignCurrencies-Yes")).isDefined must be(true)
      Option(doc.getElementById("usesForeignCurrencies-No")).isDefined must be(true)
    }

//    "show errors in the correct locations" in new ViewFixture {
//
//      val form2: InvalidForm = InvalidForm(Map.empty,
//        Seq(
//          (Path \ "blah") -> Seq(ValidationError("not a message Key")),
//          (Path \ "blah2") -> Seq(ValidationError("second not a message Key")),
//          (Path \ "blah3") -> Seq(ValidationError("third not a message Key"))
//        ))
//
//      def view = views.html.msb.which_currencies(form2, true)
//
//      errorSummary.html() must include("not a message Key")
//      errorSummary.html() must include("second not a message Key")
//      errorSummary.html() must include("third not a message Key")
//
//      doc.getElementById("id1")
//        .getElementsByClass("error-notification").first().html() must include("not a message Key")
//
//      doc.getElementById("id2")
//        .getElementsByClass("error-notification").first().html() must include("second not a message Key")
//
//      doc.getElementById("id3")
//        .getElementsByClass("error-notification").first().html() must include("third not a message Key")
//
//    }
  }
}