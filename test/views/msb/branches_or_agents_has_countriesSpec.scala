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
import models.moneyservicebusiness.BranchesOrAgentsHasCountries
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.msb.branches_or_agents

class branches_or_agents_has_countriesSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks {
    lazy val branches_or_agents = app.injector.instanceOf[branches_or_agents]
    implicit val requestWithToken = addTokenForView()
  }

  "branches_or_agents view" must {

    "have the back link button" in new ViewFixture {
      val form2: ValidForm[BranchesOrAgentsHasCountries] = Form2(BranchesOrAgentsHasCountries(true))
      def view = branches_or_agents(form2, edit = true)
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {

      val form2: ValidForm[BranchesOrAgentsHasCountries] = Form2(BranchesOrAgentsHasCountries(true))

      def view = branches_or_agents(form2, edit = true)

      doc.title must be(Messages("msb.branchesoragents.title") +
        " - " + Messages("summary.msb") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[BranchesOrAgentsHasCountries] = Form2(BranchesOrAgentsHasCountries(true))

      def view = branches_or_agents(form2, edit = true)

      heading.html must be(Messages("msb.branchesoragents.title"))
      subHeading.html must include(Messages("summary.msb"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "hasCountries") -> Seq(ValidationError("not a message Key"))
        ))

      def view = branches_or_agents(form2, edit = true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("hasCountries")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}