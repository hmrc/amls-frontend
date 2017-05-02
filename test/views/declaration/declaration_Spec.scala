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

      def view = views.html.declaration.declare(("string1","string2"), "Name")

      doc.title must startWith("string1")
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.declaration.declare(("string1","string2"), "Name")

      heading.html must be(Messages("declaration.declaration.title"))
      subHeading.html must include(Messages("string2"))

    }
  }
}
