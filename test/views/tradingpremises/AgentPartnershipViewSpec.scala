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

import forms.tradingpremises.AgentPartnershipFormProvider
import models.tradingpremises.AgentPartnership
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.AgentPartnershipView

class AgentPartnershipViewSpec extends AmlsViewSpec with Matchers {

  lazy val agent_partnership = inject[AgentPartnershipView]
  lazy val fp                = inject[AgentPartnershipFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "AgentPartnershipView" must {

    "have correct title and components" in new ViewFixture {

      def view = agent_partnership(fp().fill(AgentPartnership("Valid Name")), 1, false)

      doc.title()       must startWith(
        messages("tradingpremises.agentpartnership.title") + " - " + messages("summary.tradingpremises")
      )
      heading.html()    must include(messages("tradingpremises.agentpartnership.title"))
      subHeading.html() must include(messages("summary.tradingpremises"))

      doc.getElementById(messages("agentPartnership")).tagName() must be("input")
    }

    behave like pageWithErrors(
      agent_partnership(fp().withError("agentPartnership", "error.char.tp.agent.partnership"), 1, false),
      "agentPartnership",
      "error.char.tp.agent.partnership"
    )

    behave like pageWithBackLink(agent_partnership(fp(), 1, true))
  }
}
