/*
 * Copyright 2018 HM Revenue & Customs
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

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.tradingpremises.AgentCompanyDetails
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class agent_partnershipSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "agent_partnership view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AgentCompanyDetails] = Form2(AgentCompanyDetails("", None))

      def view = views.html.tradingpremises.agent_partnership(form2, 1, false)

      doc.title() must startWith(Messages("tradingpremises.agentpartnership.title") + " - " + Messages("summary.tradingpremises"))
      heading.html() must be(Messages("tradingpremises.agentpartnership.title"))
      subHeading.html() must include(Messages("summary.tradingpremises"))

      doc.getElementById(Messages("agentPartnership")).tagName() must be("input")
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "agentPartnership") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.agent_partnership(form2, 1, false)

      errorSummary.html() must include("not a message Key")
    }
  }
}