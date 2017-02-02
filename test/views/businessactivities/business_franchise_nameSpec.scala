package views.businessactivities

import forms.{InvalidForm, ValidForm, Form2}
import models.businessactivities.{BusinessFranchiseNo, BusinessFranchiseYes, BusinessFranchise}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class business_franchise_nameSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "business_franchise_name view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[BusinessFranchise] = Form2(BusinessFranchiseYes("Franchise name"))

      def view = views.html.businessactivities.business_franchise_name(form2, true)

      doc.title must startWith(Messages("businessactivities.businessfranchise.title") + " - " + Messages("summary.businessactivities"))
    }

    "have correct headings" in new ViewFixture {
      val form2: ValidForm[BusinessFranchise] = Form2(BusinessFranchiseNo)

      def view = views.html.businessactivities.business_franchise_name(form2, true)

      heading.html must be(Messages("businessactivities.businessfranchise.title"))
      subHeading.html must include(Messages("summary.businessactivities"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "franchiseName") -> Seq(ValidationError("not a message Key")),
          (Path \ "businessFranchise") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.businessactivities.business_franchise_name(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("franchiseName-panel")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("businessFranchise")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")
    }
  }
}
