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

  "experience_training view" must {

    "have correct title" in new ViewFixture {

      val form2: ValidForm[AgentCompanyDetails] = Form2(AgentCompanyDetails("", ""))

      def view = views.html.tradingpremises.agent_company_details(form2, 0, false)

      doc.title() must startWith(Messages("tradingpremises.youragent.company.details.title") + " - " + Messages("summary.tradingpremises"))

    }

    "have correct heading" in new ViewFixture {

      val form2: ValidForm[AgentCompanyDetails] = Form2(AgentCompanyDetails("", ""))

      def view = views.html.tradingpremises.agent_company_details(form2, 0, false)

      heading.html() must be(Messages("tradingpremises.youragent.company.details.title"))
    }

    "show errors in correct places when validation fails" in new ViewFixture {

      val messageKey1 = "definitely not a message key"
      val messageKey2 = "definitely not another message key"

      val agentCompanyNameField = "agentCompanyName"
      val agentCompanyCRNField = "agentCompanyCRNField"

      val form2: InvalidForm = InvalidForm(
        Map("a" -> Seq("a")),
        Seq((Path \ agentCompanyNameField, Seq(ValidationError(messageKey1))),
          (Path \ agentCompanyCRNField, Seq(ValidationError(messageKey2)))))

      def view = views.html.tradingpremises.agent_company_details(form2, 0, false)

      errorSummary.html() must include(messageKey1)
      errorSummary.html() must include(messageKey2)

      doc.getElementsByClass("form-field--error").first().html() must include(messageKey1)
      doc.html() must include("dhjkuyftxdrxtcfgh")
    }
  }
}
