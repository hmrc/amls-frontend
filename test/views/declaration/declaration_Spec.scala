package views.declaration

import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import play.api.i18n.Messages
import views.Fixture


class declaration_Spec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "declaration view" must {
    "have correct title" in new ViewFixture {
      def view = views.html.declaration.declare(("string1", "string2"), "Name")

      doc.title mustBe s"string1 - ${Messages("title.amls")} - ${Messages("title.gov")}"
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.declaration.declare(("string1", "string2"), "Name")

      heading.html must be(Messages("declaration.declaration.title"))
      subHeading.html must include(Messages("string2"))
    }

    "replay the person's name in the first line of the declaration text" in new ViewFixture {
      val name = "Some Person"
      def view = views.html.declaration.declare(("string1", "string2"), name)

      doc.select(".notice").text() must include(Messages("declaration.declaration.fullname", name))
    }

  }
}
