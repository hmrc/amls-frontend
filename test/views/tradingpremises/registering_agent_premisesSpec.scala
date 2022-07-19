/*
 * Copyright 2022 HM Revenue & Customs
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

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.registering_agent_premises


class registering_agent_premisesSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val registering_agent_premises = app.injector.instanceOf[registering_agent_premises]
    implicit val requestWithToken = addTokenForView()
  }

  "is_residential view" must {

      "have correct title, heading, back link and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.agent.premises.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = registering_agent_premises(form2, 1, false)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.agent.premises.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

        doc.getElementsByAttributeValue("class", "link-back") must not be empty
        doc.select("input[type=radio]").size() must be(2)

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = registering_agent_premises(form2, 1, true)

      errorSummary.html() must include("not a message Key")
    }
  }
}