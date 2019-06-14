/*
 * Copyright 2019 HM Revenue & Customs
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

class agent_company_nameSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "agent_company_name view" must {
    "have correct title, components and back link" in new ViewFixture {

      val form2: ValidForm[AgentCompanyDetails] = Form2(AgentCompanyDetails("", None))

      def view = views.html.tradingpremises.agent_company_name(form2, 1, false)

      doc.title() must startWith(Messages("tradingpremises.agentcompanyname.title") + " - " + Messages("summary.tradingpremises"))
      heading.html() must include(Messages("tradingpremises.agentcompanyname.title"))
      subHeading.html() must include(Messages("summary.tradingpremises"))
      doc.getElementsByAttributeValue("class", "link-back") must not be empty

      doc.getElementById(Messages("agentCompanyName")).tagName() must be("input")
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "agentCompanyName") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.agent_company_name(form2, 1, false)

      errorSummary.html() must include("not a message Key")
    }
  }
}