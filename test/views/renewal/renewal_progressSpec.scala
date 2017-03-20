package views.renewal

import models.registrationprogress.{Completed, Section}
import utils.GenericTestHelper
import views.Fixture

class renewal_progressSpec extends GenericTestHelper {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val renewalSection = Section("renewal", Completed, true, controllers.renewal.routes.SummaryController.get())

  }

  "The renewal progress view" must {

    "enable the submit registration button" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(renewalSection, Seq.empty, true)

      doc.select("form button[name=submit]").hasAttr("disabled") mustBe false
    }

    "disable the submit registration button" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(renewalSection, Seq.empty, false)

      doc.select("form button[name=submit]").hasAttr("disabled") mustBe true
    }

  }

}
