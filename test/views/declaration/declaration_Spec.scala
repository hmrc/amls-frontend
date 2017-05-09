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
      def view = views.html.declaration.declare("string1", "string2", "Name", isAmendment = false)

      doc.title mustBe s"string1 - ${Messages("title.amls")} - ${Messages("title.gov")}"
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.declaration.declare("string1", "string2", "Name", isAmendment = false)

      heading.html must be(Messages("declaration.declaration.title"))
      subHeading.html must include(Messages("string2"))
    }

    "have correct content" in new ViewFixture {
      def view = views.html.declaration.declare("string1", "string2", "Name", isAmendment = false)

      doc.text() must include(Messages("declaration.declaration.declare"))

      Seq(
        Messages("declaration.declaration.tellhmrc"),
        Messages("declaration.declaration.noncompliance"),
        Messages("declaration.declaration.confirm"),
        Messages("declaration.declaration.correctinformation")
      ) foreach { msg =>
        doc.select(".list.list-bullet").text() must include(msg)
      }
    }

    "have correct preamble when an 'amendment' message is passed in" in new ViewFixture {
      def view = views.html.declaration.declare("string1", "string2", "Name", isAmendment = true)

      doc.text() must include(Messages("declaration.declaration.amendment.correctinformation"))
    }

    "replay the person's name in the first line of the declaration text" in new ViewFixture {
      val name = "Some Person"
      def view = views.html.declaration.declare("string1", "string2", name, isAmendment = false)

      doc.select(".notice").text() must include(Messages("declaration.declaration.fullname", name))
    }

  }
}
