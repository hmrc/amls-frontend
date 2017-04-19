package views.estateagentbusiness

import models.estateagentbusiness._
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import play.api.i18n.Messages
import views.Fixture

class summarySpec extends GenericTestHelper with MustMatchers  {

  trait TestFixture extends Fixture {
      implicit val requestWithToken = addToken(request)
    val validBusiness = EstateAgentBusiness(
      Some(Services(Set(Commercial, AssetManagement, Residential))),
      Some(ThePropertyOmbudsman),
      Some(ProfessionalBodyYes("some body")),
      Some(PenalisedUnderEstateAgentsActNo),
      hasChanged = true
    )
  }

  "summary view" must {
    "have correct title" in new TestFixture {

      def view = views.html.estateagentbusiness.summary(validBusiness)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.estateagentbusiness"))
    }

    "have correct headings" in new TestFixture {

      def view = views.html.estateagentbusiness.summary(validBusiness)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.estateagentbusiness"))

    }

    "display the results of the form" in new TestFixture {

      def view = views.html.estateagentbusiness.summary(validBusiness)

      val checkYourAnswersSection = doc.select("section.check-your-answers")

      checkYourAnswersSection.size() must not be 0

      checkYourAnswersSection.get(0).html must include(Messages("estateagentbusiness.service.lbl.02")) // Commercial
      checkYourAnswersSection.get(0).html must include(Messages("estateagentbusiness.service.lbl.06")) // Asset management
      checkYourAnswersSection.get(0).html must include(Messages("estateagentbusiness.service.lbl.01")) // Residential

      checkYourAnswersSection.get(1).html must include(Messages("estateagentbusiness.registered.redress.title"))
      checkYourAnswersSection.get(1).html must include(Messages("estateagentbusiness.redress.lbl.01"))

      checkYourAnswersSection.get(2).html must include(Messages("estateagentbusiness.penalisedbyprofessional.title"))
      checkYourAnswersSection.get(2).html must include("some body")

      checkYourAnswersSection.get(3).html must include(Messages("estateagentbusiness.penalisedunderestateagentsact.title"))
      checkYourAnswersSection.get(3).html must include(Messages("lbl.no"))
    }

    "not display the residential section if the business does not offer residential services" in new TestFixture {

      val business = validBusiness.copy(services = Some(Services(Set(Commercial))))

      def view = views.html.estateagentbusiness.summary(business)

      val section = doc.select("section.check-your-answers")

      section.size must be(3)
      section.html() must not include Messages("estateagentbusiness.registered.redress.title")

    }
  }
}