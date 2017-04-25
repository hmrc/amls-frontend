package views.tradingpremises

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{AccountancyServices, BillPaymentServices, BusinessActivities, EstateAgentBusinessService}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import views.Fixture


class what_does_your_business_doSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "what_does_your_business_do view" must {

    val businessMatchingActivitiesAll = BusinessMatchingActivities(
      Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))

    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.whatdoesyourbusinessdo.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")



      def view = views.html.tradingpremises.what_does_your_business_do(form2, businessMatchingActivitiesAll, false ,1 )

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.whatdoesyourbusinessdo.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.select("input[type=checkbox]").size must be(3)

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.what_does_your_business_do(form2, businessMatchingActivitiesAll, true,1)

      errorSummary.html() must include("not a message Key")
    }
  }
}