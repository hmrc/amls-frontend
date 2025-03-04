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

package views.msb

import forms.msb.BranchesOrAgentsFormProvider
import models.moneyservicebusiness.BranchesOrAgentsHasCountries
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.msb.BranchesOrAgentsView

class BranchesOrAgentsViewSpec extends AmlsViewSpec with Matchers {

  lazy val branches_or_agents = inject[BranchesOrAgentsView]
  lazy val fp                 = inject[BranchesOrAgentsFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "BranchesOrAgentsView view" must {

    "have correct title" in new ViewFixture {

      def view = branches_or_agents(fp().fill(BranchesOrAgentsHasCountries(true)), edit = true)

      doc.title must be(
        messages("msb.branchesoragents.title") +
          " - " + messages("summary.msb") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = branches_or_agents(fp().fill(BranchesOrAgentsHasCountries(false)), edit = true)

      heading.html    must be(messages("msb.branchesoragents.title"))
      subHeading.html must include(messages("summary.msb"))
    }

    behave like pageWithErrors(
      branches_or_agents(
        fp().withError("hasCountries", "error.required.hasCountries.msb.branchesOrAgents"),
        false
      ),
      "hasCountries",
      "error.required.hasCountries.msb.branchesOrAgents"
    )

    behave like pageWithBackLink(branches_or_agents(fp(), false))
  }
}
