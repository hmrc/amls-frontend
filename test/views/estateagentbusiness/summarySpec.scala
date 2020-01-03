/*
 * Copyright 2020 HM Revenue & Customs
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

package views.estateagentbusiness

import forms.EmptyForm
import models.estateagentbusiness._
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import play.api.i18n.Messages
import views.Fixture

class summarySpec extends AmlsViewSpec with MustMatchers  {

  trait TestFixture extends Fixture {
      implicit val requestWithToken = addTokenForView()
    val validBusiness = EstateAgentBusiness(
      Some(Services(Set(Commercial, AssetManagement, Residential))),
      Some(ThePropertyOmbudsman),
      Some(ProfessionalBodyYes("some body")),
      Some(PenalisedUnderEstateAgentsActNo),
      hasChanged = true
    )
  }

  "summary view" must {
    "have correct title" in new TestFixture {

      def view = views.html.estateagentbusiness.summary(EmptyForm, validBusiness)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.estateagentbusiness"))
    }

    "have correct headings" in new TestFixture {

      def view = views.html.estateagentbusiness.summary(EmptyForm, validBusiness)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.estateagentbusiness"))

    }

    "display the results of the form" in new TestFixture {

      def view = views.html.estateagentbusiness.summary(EmptyForm, validBusiness)

      val checkYourAnswersSection = doc.select("div.cya-summary-list__row")

      checkYourAnswersSection.size() must not be 0

      checkYourAnswersSection.get(0).html must include(Messages("estateagentbusiness.service.lbl.02")) // Commercial
      checkYourAnswersSection.get(0).html must include(Messages("estateagentbusiness.service.lbl.06")) // Asset management
      checkYourAnswersSection.get(0).html must include(Messages("estateagentbusiness.service.lbl.01")) // Residential

      checkYourAnswersSection.get(1).html must include(Messages("estateagentbusiness.registered.redress.title"))
      checkYourAnswersSection.get(1).html must include(Messages("estateagentbusiness.redress.lbl.01"))

      checkYourAnswersSection.get(2).html must include(Messages("estateagentbusiness.penalisedbyprofessional.title"))
      checkYourAnswersSection.get(2).html must include("some body")

      checkYourAnswersSection.get(3).html must include(Messages("estateagentbusiness.penalisedunderestateagentsact.title"))
      checkYourAnswersSection.get(3).html must include(Messages("lbl.no"))
    }

    "display the business is not registered with redress scheme" in new TestFixture {

      val business = validBusiness.copy(redressScheme = Some(RedressSchemedNo))

      def view = views.html.estateagentbusiness.summary(EmptyForm, business)

      val checkYourAnswersSection = doc.select("div.cya-summary-list__row")

      checkYourAnswersSection.size() must be(4)
      checkYourAnswersSection.get(1).html must include(Messages("estateagentbusiness.registered.redress.title"))
      checkYourAnswersSection.get(1).html must include(Messages("estateagentbusiness.redress.lbl.05"))
    }

    "not display the residential section if the business does not offer residential services" in new TestFixture {

      val business = validBusiness.copy(services = Some(Services(Set(Commercial))))

      def view = views.html.estateagentbusiness.summary(EmptyForm, business)

      val section = doc.select("span.bold")

      section.size must be(3)
      section.html() must not include Messages("estateagentbusiness.registered.redress.title")

    }
  }
}