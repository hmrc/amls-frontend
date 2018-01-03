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
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import models.tradingpremises.AgentCompanyDetails
import play.api.i18n.Messages
import views.Fixture

class agent_company_detailsSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "add_person view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AgentCompanyDetails] = Form2(AgentCompanyDetails("", None))

      def view = views.html.tradingpremises.agent_company_details(form2, 0, false)

      doc.title() must startWith(Messages("tradingpremises.youragent.company.details.title") + " - " + Messages("summary.tradingpremises"))

    }

    "have correct heading" in new ViewFixture {

      val form2: ValidForm[AgentCompanyDetails] = Form2(AgentCompanyDetails("", None))

      def view = views.html.tradingpremises.agent_company_details(form2, 0, false)

      heading.html() must be(Messages("tradingpremises.youragent.company.details.title"))
      subHeading.html() must include(Messages("summary.tradingpremises"))
    }


    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "agentCompanyName") -> Seq(ValidationError("not a message Key")),
          (Path \ "companyRegistrationNumber") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.tradingpremises.agent_company_details(form2, 0, false)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("agentCompanyName")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("companyRegistrationNumber")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")
    }
  }
}