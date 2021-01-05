/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.Country
import models.moneyservicebusiness.BranchesOrAgentsWhichCountries
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.msb.branches_or_agents_which_countries

class branches_or_agents_with_countriesSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks {
    lazy val branches_or_agents_which_countries = app.injector.instanceOf[branches_or_agents_which_countries]
    implicit val requestWithToken = addTokenForView()
  }

  "branches_or_agents view" must {

    "have the back link button" in new ViewFixture {
      val form2: ValidForm[BranchesOrAgentsWhichCountries] = Form2(BranchesOrAgentsWhichCountries(Seq.empty[Country]))
      def view = branches_or_agents_which_countries(form2, edit = true, mockAutoComplete.getCountries)
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {

      val form2: ValidForm[BranchesOrAgentsWhichCountries] = Form2(BranchesOrAgentsWhichCountries(Seq.empty[Country]))

      def view = branches_or_agents_which_countries(form2, edit = true, mockAutoComplete.getCountries)

      doc.title must be(Messages("msb.branchesoragents.countries.title") +
        " - " + Messages("summary.msb") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[BranchesOrAgentsWhichCountries] = Form2(BranchesOrAgentsWhichCountries(Seq.empty[Country]))

      def view = branches_or_agents_which_countries(form2, edit = true, mockAutoComplete.getCountries)

      heading.html must be(Messages("msb.branchesoragents.countries.title"))
      subHeading.html must include(Messages("summary.msb"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "countries") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = branches_or_agents_which_countries(form2, edit = true, mockAutoComplete.getCountries)

      errorSummary.html() must include("second not a message Key")

      doc.getElementById("countries")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}