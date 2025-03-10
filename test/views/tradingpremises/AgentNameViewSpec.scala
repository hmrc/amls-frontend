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

import forms.tradingpremises.AgentNameFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.AgentNameView

class AgentNameViewSpec extends AmlsViewSpec with Matchers {

  lazy val agent_name: AgentNameView = inject[AgentNameView]
  lazy val fp: AgentNameFormProvider = inject[AgentNameFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "have correct title and heading" in new ViewFixture {

    def view: _root_.play.twirl.api.HtmlFormat.Appendable =
      agent_name(fp(), 0, false)

    doc.title()    must startWith(
      messages("tradingpremises.agentname.title") + " - " + messages("summary.tradingpremises")
    )
    heading.html() must be(messages("tradingpremises.agentname.title"))
  }

  "include date of birth" in new ViewFixture {

    def view: _root_.play.twirl.api.HtmlFormat.Appendable =
      agent_name(fp(), 0, false)

    doc.html() must include(messages("tradingpremises.agentname.name.dateOfBirth.lbl"))
  }

  behave like pageWithErrors(
    agent_name(fp().withError("agentName", "error.char.tp.agent.name"), 1, true),
    "agentName",
    "error.char.tp.agent.name"
  )

  behave like pageWithErrors(
    agent_name(fp().withError("agentDateOfBirth", "error.required.tp.agent.date.all"), 2, false),
    "agentDateOfBirth",
    "error.required.tp.agent.date.all"
  )

  behave like pageWithBackLink(agent_name(fp(), 3, false))

}
