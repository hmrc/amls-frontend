package views.tradingpremises

import forms.{Form2, InvalidForm, ValidForm}
import models.tradingpremises.AgentCompanyDetails
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture

class agent_company_detailsSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "add_person view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AgentCompanyDetails] = Form2(AgentCompanyDetails("", None))

      def view = views.html.tradingpremises.agent_company_details(form2, 0, false)

      doc.title() must startWith(Messages("tradingpremises.youragent.company.details.title") + " - " + Messages("summary.tradingpremises"))

    }

    "have correct heading" in new ViewFixture {

      val form2: ValidForm[AgentCompanyDetails] = Form2(AgentCompanyDetails("", None))

      def view = views.html.tradingpremises.agent_company_details(form2, 0, false)

      heading.html() must be(Messages("tradingpremises.youragent.company.details.title"))
      subHeading.html() must include(Messages("summary.tradingpremises"))
    }


    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "agentCompanyName") -> Seq(ValidationError("not a message Key")),
          (Path \ "companyRegistrationNumber") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.tradingpremises.agent_company_details(form2, 0, false)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("agentCompanyName")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("companyRegistrationNumber")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")
    }
  }
}