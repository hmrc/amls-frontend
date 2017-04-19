package views.businessactivities

import forms.{InvalidForm, ValidForm, Form2}
import models.businessactivities.{RiskAssessmentPolicyNo, RiskAssessmentPolicy}
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class risk_assessment_policySpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "risk_assessment_policy view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[RiskAssessmentPolicy] = Form2(RiskAssessmentPolicyNo)

      def view = views.html.businessactivities.risk_assessment_policy(form2, true)

      doc.title must startWith(Messages("businessactivities.riskassessment.policy.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[RiskAssessmentPolicy] = Form2(RiskAssessmentPolicyNo)

      def view = views.html.businessactivities.risk_assessment_policy(form2, true)

      heading.html must be(Messages("businessactivities.riskassessment.policy.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "hasPolicy") -> Seq(ValidationError("not a message Key")),
          (Path \ "riskassessments") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.businessactivities.risk_assessment_policy(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("hasPolicy")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("riskassessments")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}