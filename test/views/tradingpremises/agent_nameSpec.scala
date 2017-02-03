package views.tradingpremises

import forms.{EmptyForm, InvalidForm, ValidForm}
import models.tradingpremises.AgentName
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.api.test.FakeApplication
import utils.GenericTestHelper
import views.Fixture
import jto.validation.{Path, ValidationError}

class agent_nameSpec extends GenericTestHelper with MustMatchers {

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> false))


  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "agent_name view" must {

    "have correct title and heading" in new ViewFixture {

      def view: _root_.play.twirl.api.HtmlFormat.Appendable =
        views.html.tradingpremises.agent_name(EmptyForm, 0, false)

      doc.title() must startWith(Messages("tradingpremises.agentname.title") + " - " + Messages("summary.tradingpremises"))
      heading.html() must be(Messages("tradingpremises.agentname.title"))

    }

    "not include date of birth" in new ViewFixture {

      def view: _root_.play.twirl.api.HtmlFormat.Appendable =
        views.html.tradingpremises.agent_name(EmptyForm, 0, false)

      doc.html() must not include (Messages("tradingpremises.agentname.name.dateOfBirth.lbl"))
    }

    "show errors in correct places when validation fails" in new ViewFixture {

      val messageKey1 = "definitely not a message key"
      val agentNameField = "agentName"
      val form2: InvalidForm = InvalidForm(Map("thing" -> Seq("thing")),
        Seq((Path \ agentNameField, Seq(ValidationError(messageKey1)))))

      def view: _root_.play.twirl.api.HtmlFormat.Appendable =
        views.html.tradingpremises.agent_name(form2, 0, false)

      errorSummary.html() must include(messageKey1)
      doc.getElementById(agentNameField).parent().getElementsByClass("error-notification").first().html() must include(messageKey1)

    }
  }

}

class agent_nameSpecR7 extends GenericTestHelper with MustMatchers {

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> true))


  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }


  "include date of birth" in new ViewFixture {

    def view: _root_.play.twirl.api.HtmlFormat.Appendable =
      views.html.tradingpremises.agent_name(EmptyForm, 0, false)

    doc.html() must include(Messages("tradingpremises.agentname.name.dateOfBirth.lbl"))
  }

  "show errors in correct places when validation fails" in new ViewFixture {

    val messageKey1 = "definitely not a message key"
    val agentDateOfBirth = "agentDateOfBirth"
    val form2: InvalidForm = InvalidForm(Map("thing" -> Seq("thing")),
      Seq((Path \ agentDateOfBirth, Seq(ValidationError(messageKey1)))))

    def view: _root_.play.twirl.api.HtmlFormat.Appendable =
      views.html.tradingpremises.agent_name(form2, 0, false)

    errorSummary.html() must include(messageKey1)
    doc.getElementById(agentDateOfBirth).parent().getElementsByClass("error-notification").first().html() must include(messageKey1)

  }

}
