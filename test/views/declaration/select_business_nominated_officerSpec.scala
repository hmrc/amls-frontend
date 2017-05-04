package views.declaration

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.declaration.BusinessNominatedOfficer
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import models.responsiblepeople.ResponsiblePeople
import play.api.i18n.Messages
import views.Fixture

class select_business_nominated_officerSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "select_business_nominated_officer view" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view = views.html.declaration.select_business_nominated_officer("subheading", EmptyForm, Seq.empty[ResponsiblePeople])

      doc.title mustBe s"${Messages("declaration.who.is.business.nominated.officer")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      heading.html must be(Messages("declaration.who.is.business.nominated.officer"))
      subHeading.html must include("subheading")
      //code to check existance of form fields			
    }

  }
}
