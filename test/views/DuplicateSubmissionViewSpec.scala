package views

import play.api.i18n.Messages
import play.twirl.api.Html
import utils.GenericTestHelper

class DuplicateSubmissionViewSpec extends GenericTestHelper {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val getHelpView = Html("<p>Get help here</p>")

    def view = views.html.duplicate_submission(getHelpView)

  }

  "The html content" must {
    "have the correct title and subtitles" in new ViewFixture {

      doc.title mustBe s"""${Messages("error.submission.duplicate.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"""

    }

    "have the correct content" in new ViewFixture {
      import utils.Strings._

      doc.getElementById("page-content").html.replace("\n","") mustBe Messages("error.submission.duplicate.content").paragraphize
    }
  }

}
