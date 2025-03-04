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

import forms.msb.BranchesOrAgentsWhichCountriesFormProvider
import models.Country
import models.moneyservicebusiness.BranchesOrAgentsWhichCountries
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.msb.BranchesOrAgentsWhichCountriesView

class BranchesOrAgentsWhichCountriesViewSpec extends AmlsViewSpec with Matchers with AutoCompleteServiceMocks {

  lazy val countriesView = inject[BranchesOrAgentsWhichCountriesView]
  lazy val fp            = inject[BranchesOrAgentsWhichCountriesFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "BranchesOrAgentsWhichCountriesView" must {

    "have correct title" in new ViewFixture {

      def view = countriesView(
        fp().fill(BranchesOrAgentsWhichCountries(Seq.empty[Country])),
        edit = true,
        mockAutoComplete.formOptions
      )

      doc.title must be(
        messages("msb.branchesoragents.countries.title") +
          " - " + messages("summary.msb") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = countriesView(
        fp().fill(BranchesOrAgentsWhichCountries(Seq.empty[Country])),
        edit = true,
        mockAutoComplete.formOptions
      )

      heading.html    must be(messages("msb.branchesoragents.countries.title"))
      subHeading.html must include(messages("summary.msb"))

    }

    behave like pageWithErrors(
      countriesView(
        fp().withError("countries[0]", "error.invalid.countries.msb.branchesOrAgents.country"),
        edit = true,
        mockAutoComplete.formOptions
      ),
      "location-autocomplete-0",
      "error.invalid.countries.msb.branchesOrAgents.country"
    )

    behave like pageWithBackLink(countriesView(fp(), false, mockAutoComplete.formOptions))
  }
}
