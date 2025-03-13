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

import forms.tradingpremises.RegisteringAgentPremisesFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.RegisteringAgentPremisesView

class RegisteringAgentPremisesViewSpec extends AmlsViewSpec with Matchers {

  lazy val registering_agent_premises = inject[RegisteringAgentPremisesView]
  lazy val fp                         = inject[RegisteringAgentPremisesFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "RegisteringAgentPremisesView" must {

    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val pageTitle = messages("tradingpremises.agent.premises.title") + " - " +
        messages("summary.tradingpremises") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      def view = registering_agent_premises(fp(), 1, false)

      doc.title       must be(pageTitle)
      heading.html    must be(messages("tradingpremises.agent.premises.title"))
      subHeading.html must include(messages("summary.tradingpremises"))

      doc.select("input[type=radio]").size() must be(2)
    }

    behave like pageWithErrors(
      registering_agent_premises(
        fp().withError("agentPremises", "error.required.tp.agent.premises"),
        1,
        true
      ),
      "agentPremises",
      "error.required.tp.agent.premises"
    )

    behave like pageWithBackLink(registering_agent_premises(fp(), 1, false))
  }
}
