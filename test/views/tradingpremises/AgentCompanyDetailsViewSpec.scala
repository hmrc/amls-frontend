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

import forms.tradingpremises.AgentCompanyDetailsFormProvider
import org.scalatest.matchers.must.Matchers
import utils.AmlsViewSpec
import models.tradingpremises.AgentCompanyDetails
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import views.Fixture
import views.html.tradingpremises.AgentCompanyDetailsView

class AgentCompanyDetailsViewSpec extends AmlsViewSpec with Matchers {

  lazy val agent_company_details = inject[AgentCompanyDetailsView]
  lazy val fp                    = inject[AgentCompanyDetailsFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "AgentCompanyDetailsView" must {
    "have correct title" in new ViewFixture {

      def view = agent_company_details(fp().fill(AgentCompanyDetails("A Name", Some("QWER1234"))), 0, false)

      doc.title() must startWith(
        messages("tradingpremises.youragent.company.details.title") + " - " + messages("summary.tradingpremises")
      )

    }

    "have correct heading" in new ViewFixture {

      def view = agent_company_details(fp(), 0, false)

      heading.html()    must be(messages("tradingpremises.youragent.company.details.title"))
      subHeading.html() must include(messages("summary.tradingpremises"))
    }

    behave like pageWithErrors(
      agent_company_details(
        fp().withError("agentCompanyName", "error.invalid.char.tp.agent.company.details"),
        0,
        false
      ),
      "agentCompanyName",
      "error.invalid.char.tp.agent.company.details"
    )

    behave like pageWithErrors(
      agent_company_details(
        fp().withError("companyRegistrationNumber", "error.char.to.agent.company.reg.number"),
        0,
        false
      ),
      "companyRegistrationNumber",
      "error.char.to.agent.company.reg.number"
    )

    behave like pageWithBackLink(agent_company_details(fp(), 1, true))
  }
}
