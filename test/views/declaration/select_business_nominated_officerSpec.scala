package views.declaration

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.declaration.BusinessNominatedOfficer
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import play.api.i18n.Messages
import views.Fixture
import cats.implicits._

class select_business_nominated_officerSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "select_business_nominated_officer view" must {
    "have correct title, headings and content" in new ViewFixture {

      def view = views.html.declaration.select_business_nominated_officer("subheading", EmptyForm, Seq.empty[ResponsiblePeople])

      doc.title mustBe s"${Messages("declaration.who.is.business.nominated.officer")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      heading.html must be(Messages("declaration.who.is.business.nominated.officer"))
      subHeading.html must include("subheading")
    }

    "have a list of responsible people" in new ViewFixture {

      val people = Seq(
        ResponsiblePeople(PersonName("Test", None, "Person1", None, None).some),
        ResponsiblePeople(PersonName("Test", None, "Person2", None, None).some)
      )

      def view = views.html.declaration.select_business_nominated_officer("subheading", EmptyForm, people)

      people map(_.personName.get) foreach { n =>
        val id = s"value-${n.firstName}${n.lastName}"
        val e = doc.getElementById(id)

        Option(e) must be(defined)
        e.`val` mustBe s"${n.firstName}${n.lastName}"

        val label = doc.select(s"label[for=$id]")
        label.text() must include(n.fullName)
      }

    }

    "prepopulate the selected nominated officer" in new ViewFixture {

      val people = Seq(
        ResponsiblePeople(PersonName("Test", None, "Person1", None, None).some),
        ResponsiblePeople(PersonName("Test", None, "Person2", None, None).some)
      )

      val f = Form2(BusinessNominatedOfficer("TestPerson1"))

      def view = views.html.declaration.select_business_nominated_officer("subheading", f, people)

      doc.select("input[type=radio][id=value-TestPerson1").hasAttr("checked") mustBe true

    }

  }
}
