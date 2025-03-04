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

package views.businessmatching

import forms.businessmatching.RegisterBusinessActivitiesFormProvider
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities, BusinessActivity}
import org.scalatest.matchers.must.Matchers
import play.api.data.{Form, FormError}
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.RegisterServicesView

class RegisterServicesViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture {
    lazy val formProvider                                          = app.injector.instanceOf[RegisterBusinessActivitiesFormProvider]
    lazy val register_services                                     = app.injector.instanceOf[RegisterServicesView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "RegisterServicesView" must {
    "have correct title" when {
      "pre-submission" in new ViewFixture {

        val filledForm: Form[Seq[BusinessActivity]] = formProvider().fill(Seq(AccountancyServices))

        def view = register_services(filledForm, edit = true, Seq(AccountancyServices), isPreSubmission = true)

        doc.title       must startWith(
          messages("businessmatching.registerservices.title") + " - " + messages("summary.businessmatching")
        )
        heading.html    must be(messages("businessmatching.registerservices.title"))
        subHeading.html must include(messages("summary.businessmatching"))

      }

      "post-submission" in new ViewFixture {

        val filledForm: Form[Seq[BusinessActivity]] = formProvider().fill(Seq(AccountancyServices))

        def view = register_services(filledForm, edit = true, Seq.empty, isPreSubmission = false)

        doc.title       must startWith(
          messages("businessmatching.registerservices.other.title") + " - " + messages("summary.businessmatching")
        )
        heading.html    must be(messages("businessmatching.registerservices.other.title"))
        subHeading.html must include(messages("summary.businessmatching"))

      }
    }

    "notify of services already selected" when {
      "status is post submission" in new ViewFixture {

        def view = register_services(formProvider(), edit = true, Seq(AccountancyServices), isPreSubmission = false)

        html must include(messages("businessmatching.registerservices.existing"))

      }
    }

    "show errors in the correct locations" in new ViewFixture {

      val messageKey = "error.required.bm.register.service"

      val filledForm: Form[Seq[BusinessActivity]] = formProvider().withError(FormError("value", messageKey))

      def view = register_services(filledForm, edit = true, Seq.empty, isPreSubmission = true)

      doc.getElementsByClass("govuk-list govuk-error-summary__list").first.text() mustBe messages(messageKey)

      doc.getElementById("value-error").text() mustBe s"Error: ${messages(messageKey)}"

    }

    "hide the return to progress link" in new ViewFixture {
      def view =
        register_services(formProvider(), edit = true, Seq.empty, isPreSubmission = true, showReturnLink = false)

      doc.body().text() must not include messages("link.return.registration.progress")
    }

    "have a back link in pre-submission mode" in new ViewFixture {
      def view = register_services(formProvider(), edit = true, Seq.empty, isPreSubmission = true)

      doc.getElementsByClass("govuk-back-link").text() must be("Back")
    }

    "have a back link in non pre-submission mode" in new ViewFixture {
      def view = register_services(formProvider(), edit = true, Seq.empty, isPreSubmission = false)

      doc.getElementsByClass("govuk-back-link").text() must be("Back")

    }

    "have correct hint content" in new ViewFixture {

      val filledForm: Form[Seq[BusinessActivity]] = formProvider().fill(BusinessActivities.all.toSeq)

      def view = register_services(filledForm, edit = true, Seq.empty, isPreSubmission = false)

      doc.html must include(
        "They provide services like professional bookkeeping, accounts preparation and signing, and tax advice."
      )
      doc.html must include(
        "They facilitate and engage in the selling of art for €10,000 or more. Roles include things like art agents, art auctioneers, art dealers, and gallery owners."
      )
      doc.html must include("They handle payments for utility and other household bills on behalf of customers.")
      doc.html must include(
        "This includes estate agency activities, like sending out property details and arranging viewings. It also includes lettings when the monthly rent for a property is €10,000 or more."
      )
      doc.html must include(
        "They accept or make cash payments of €10,000 or more (or equivalent) in exchange for goods. This includes when a customer deposits cash directly into a bank account. Estate agents are not classed as high value dealers."
      )
      doc.html must include("They exchange currency, transmit money, or cash cheques for their customers.")
      doc.html must include(
        "They form companies, and supply services like providing a business or correspondence address. They also act, or arrange for another person to act, in a certain role. For example, a director in a company or a trustee of an express trust."
      )
      doc.html must include(
        "They act as a link between customers and suppliers. They handle payments made through devices like mobile phones, computers, and smart TVs."
      )
    }
  }
}
