/*
 * Copyright 2024 HM Revenue & Customs
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

import forms.tradingpremises.RemoveAgentPremisesReasonsFormProvider
import models.tradingpremises._
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.RemoveAgentPremisesReasonsView

class RemoveAgentPremisesReasonsViewSpec extends AmlsViewSpec {

  import models.tradingpremises.RemovalReasonConstants._

  lazy val remove_agent_premises_reasons = inject[RemoveAgentPremisesReasonsView]
  lazy val fp                            = inject[RemoveAgentPremisesReasonsFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "RemoveAgentPremisesReasonsView" must {
    "have correct title" in new ViewFixture {

      def view = remove_agent_premises_reasons(fp().fill(AgentRemovalReason(Schema.MAJOR_COMPLIANCE_ISSUES)), 1, false)

      doc.title must startWith(messages("tradingpremises.remove_reasons.agent.premises.title"))
    }

    "have correct headings" in new ViewFixture {

      def view = remove_agent_premises_reasons(fp().fill(AgentRemovalReason(Schema.MAJOR_COMPLIANCE_ISSUES)), 1, true)

      heading.html    must be(messages("tradingpremises.remove_reasons.agent.premises.title"))
      subHeading.html must include(messages("summary.tradingpremises"))

    }

    behave like pageWithErrors(
      remove_agent_premises_reasons(
        fp().withError("removalReason", "tradingpremises.remove_reasons.missing"),
        0,
        false
      ),
      "removalReason",
      "tradingpremises.remove_reasons.missing"
    )

    behave like pageWithErrors(
      remove_agent_premises_reasons(
        fp().withError("removalReasonOther", "tradingpremises.remove_reasons.agent.other.missing"),
        0,
        false
      ),
      "removalReasonOther",
      "tradingpremises.remove_reasons.agent.other.missing"
    )

    behave like pageWithBackLink(remove_agent_premises_reasons(fp(), 0, false))
  }
}
