/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views.tradingpremises

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.tradingpremises._
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.GenericTestHelper
import views.Fixture

class remove_agent_premises_reasonsSpec extends GenericTestHelper {

  import models.tradingpremises.RemovalReasonConstants._

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "remove_agent_premises_reasons view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AgentRemovalReason] = Form2(AgentRemovalReason(Schema.MAJOR_COMPLIANCE_ISSUES))

      def view = views.html.tradingpremises.remove_agent_premises_reasons(form2, 1, false)

      doc.title must startWith(Messages("tradingpremises.remove_reasons.agent.premises.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[AgentRemovalReason] = Form2(AgentRemovalReason(Schema.MAJOR_COMPLIANCE_ISSUES))

      def view = views.html.tradingpremises.remove_agent_premises_reasons(form2, 1, true)

      heading.html must be(Messages("tradingpremises.remove_reasons.agent.premises.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

    }

    "show errors in the correct locations" when {

      "nothing is selected" in new ViewFixture {

        val field = "removalReason"
        val errorKey = "error.missing"

        val invalidForm: InvalidForm = InvalidForm(Map.empty,
          Seq((Path \ field, Seq(ValidationError(errorKey)))))

        def view = views.html.tradingpremises.remove_agent_premises_reasons(invalidForm, 0, false)

        errorSummary.html() must include(errorKey)
        doc.getElementById(field).parent().getElementsByClass("error-notification").first().html() must include(errorKey)
      }

      "Other is selected but no other reason is given" in new ViewFixture {

        import models.tradingpremises.RemovalReasonConstants._

        val field = "removalReasonOther"
        val errorKey = "tradingpremises.remove_reasons.agent.other.missing"

        val invalidForm: InvalidForm = InvalidForm(Map("removalReason" -> Seq(Form.OTHER)),
          Seq((Path \ field, Seq(ValidationError(errorKey)))))

        def view = views.html.tradingpremises.remove_agent_premises_reasons(invalidForm, 0, false)

        errorSummary.html() must include(Messages(errorKey))
        doc.getElementById(field).parent().getElementsByClass("error-notification").first().html() must include(Messages(errorKey))
      }
    }
  }
}