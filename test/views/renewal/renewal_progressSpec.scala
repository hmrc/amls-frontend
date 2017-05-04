package views.renewal

import models.registrationprogress.{Completed, Section}
import org.joda.time.LocalDate
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture
import utils.DateHelper
import utils.Strings.TextHelpers

class renewal_progressSpec extends GenericTestHelper {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val renewalSection = Section("renewal", Completed, true, controllers.renewal.routes.SummaryController.get())

    val renewalDate = LocalDate.now().plusDays(15)

  }

  "The renewal progress view" must {

    "enable the submit registration button" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(renewalSection, Seq.empty, true, true, Some(renewalDate))

      doc.select("form button[name=submit]").hasAttr("disabled") mustBe false
    }

    "disable the submit registration button" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(renewalSection, Seq.empty, false, true,Some(renewalDate))

      doc.select("form button[name=submit]").hasAttr("disabled") mustBe true
    }

    "show intro for MSB and TCSP businesses" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(renewalSection, Seq.empty, false, true, Some(renewalDate))

      html must include (Messages("renewal.progress.tpandrp.intro", DateHelper.formatDate(renewalDate)).convertLineBreaks)
    }

    "show intro for non MSB and TCSP businesses" in new ViewFixture {

      override def view = views.html.renewal.renewal_progress(renewalSection, Seq.empty, false, false, Some(renewalDate))

      html must include (Messages("renewal.progress.tponly.intro", DateHelper.formatDate(renewalDate)).convertLineBreaks)
    }

  }

}
