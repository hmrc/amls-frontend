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

package views.businessmatching

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{AccountancyServices, ArtMarketParticipant, BillPaymentServices, BusinessActivities, HighValueDealing, MoneyServiceBusiness, TelephonePaymentService, TrustAndCompanyServices, EstateAgentBusinessService}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import utils.AmlsSpec
import views.Fixture

class register_servicesSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "register_services view" must {
    "have correct title" when {
      "pre-submission" in new ViewFixture {

        val form2: ValidForm[BusinessActivities] = Form2(BusinessActivities(Set(AccountancyServices)))

        def view = views.html.businessmatching.register_services(form2, edit = true, Seq("01"), Set.empty, isPreSubmission = true)

        doc.title must startWith(Messages("businessmatching.registerservices.title") + " - " + Messages("summary.businessmatching"))
        heading.html must be(Messages("businessmatching.registerservices.title"))
        subHeading.html must include(Messages("summary.businessmatching"))

      }
      "post-submission" in new ViewFixture {

        val form2: ValidForm[BusinessActivities] = Form2(BusinessActivities(Set(AccountancyServices)))

        def view = views.html.businessmatching.register_services(form2, edit = true, Seq("01"), Set.empty, isPreSubmission = false)

        doc.title must startWith(Messages("businessmatching.registerservices.other.title") + " - " + Messages("summary.businessmatching"))
        heading.html must be(Messages("businessmatching.registerservices.other.title"))
        subHeading.html must include(Messages("summary.businessmatching"))

      }
    }

    "notify of services already selected" when {
      "status is post submission" in new ViewFixture {

        def view = views.html.businessmatching.register_services(EmptyForm, edit = true, Seq("01"), Set.empty, isPreSubmission = false)

        html must include(Messages("businessmatching.registerservices.existing"))

      }
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "businessActivities") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.register_services(form2, edit = true, Seq("01"), Set.empty, isPreSubmission = true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("businessActivities")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
    "hide the return to progress link" in new ViewFixture {
      def view = views.html.businessmatching.register_services(EmptyForm, edit = true, Seq("01"), Set.empty, isPreSubmission = true, showReturnLink = false)

      doc.body().text() must not include Messages("link.return.registration.progress")
    }

    "have a back link in pre-submission mode" in new ViewFixture {
      def view = views.html.businessmatching.register_services(EmptyForm, edit = true, Seq("01"), Set.empty, isPreSubmission = true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have a back link in non pre-submission mode" in new ViewFixture {
      def view = views.html.businessmatching.register_services(EmptyForm, edit = true, Seq("01"), Set.empty, isPreSubmission = false)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct hint content" in new ViewFixture {
      val form2: ValidForm[BusinessActivities] = Form2(BusinessActivities(Set(
        AccountancyServices,
        MoneyServiceBusiness,
        TrustAndCompanyServices,
        TelephonePaymentService,
        ArtMarketParticipant,
        BillPaymentServices,
        EstateAgentBusinessService,
        HighValueDealing)))

      def view = views.html.businessmatching.register_services(form2, edit = true, Seq("01", "02", "03", "04", "05", "06", "07", "08"), Set.empty, isPreSubmission = false)

      doc.html must include("They provide services like professional bookkeeping, accounts preparation and signing, and tax advice.")
      doc.html must include("They facilitate and engage in the selling of art for €10,000 or more. Roles include things like art agents, art auctioneers, art dealers, and gallery owners.")
      doc.html must include("They handle payments for utility and other household bills on behalf of customers.")
      doc.html must include("They introduce and act on instructions from people who want to buy or sell property. They also secure the purchase or sale of property.")
      doc.html must include("They accept or make cash payments of €10,000 or more (or equivalent) in exchange for goods. This includes when a customer deposits cash directly into a bank account. Estate agents are not classed as high value dealers.")
      doc.html must include("They exchange currency, transmit money, or cash cheques for their customers.")
      doc.html must include("They form companies, and supply services like providing a business or correspondence address. They also act, or arrange for another person to act, in a certain role. For example, a director in a company or a trustee of an express trust.")
      doc.html must include("They act as a link between customers and suppliers. They handle payments made through devices like mobile phones, computers, and smart TVs.")
    }
  }
}