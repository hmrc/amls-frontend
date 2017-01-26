package views.businessactivities

import forms.{InvalidForm, ValidForm, Form2}
import models.businessactivities.ExpectedBusinessTurnover
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class expected_business_turnoverSpec extends GenericTestHelper with MustMatchers  {

  "expected_business_turnover view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ExpectedBusinessTurnover] = Form2(ExpectedBusinessTurnover.Third)

      def view = views.html.businessactivities.expected_business_turnover(form2, true)

      doc.title must startWith(Messages("businessactivities.business-turnover.title") + " - " + Messages("summary.businessactivities"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ExpectedBusinessTurnover] = Form2(ExpectedBusinessTurnover.Second)

      def view = views.html.businessactivities.expected_business_turnover(form2, true)

      heading.html must be(Messages("businessactivities.business-turnover.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "expectedBusinessTurnover") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessactivities.expected_business_turnover(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("expectedBusinessTurnover")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
