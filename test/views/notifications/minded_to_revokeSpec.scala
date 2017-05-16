package views.notifications

import forms.{Form2, ValidForm}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class minded_to_revokeSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {

    implicit val requestWithToken = addToken(request)

    val businessName = "Fake Name Ltd."

  }

  "branches_or_agents view" must {

    "have correct title" in new ViewFixture {

      def view = views.html.notifications.minded_to_revoke("msgContent", "amlsRegNo", businessName)

      doc.title must be(Messages("notifications.mtrv.title") +
        " - " + Messages("status.title") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.notifications.minded_to_revoke("msgContent", "amlsRegNo", businessName)

      heading.html must be(Messages("notifications.mtrv.title"))
      subHeading.html must include(Messages("status.title"))

    }

  }


}
