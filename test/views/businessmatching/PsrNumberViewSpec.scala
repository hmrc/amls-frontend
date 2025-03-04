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

import forms.businessmatching.PSRNumberFormProvider
import models.businessmatching.{BusinessAppliedForPSRNumber, BusinessAppliedForPSRNumberYes}
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.PsrNumberView

class PsrNumberViewSpec extends AmlsViewSpec {

  lazy val psr_number                                            = app.injector.instanceOf[PsrNumberView]
  lazy val formProvider                                          = app.injector.instanceOf[PSRNumberFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "psr_number view" must {
    "have correct title for pre-submission mode" in new ViewFixture {

      val filledForm = formProvider().fill(BusinessAppliedForPSRNumberYes("1234"))

      def view = psr_number(filledForm, edit = false, isPreSubmission = true)

      doc.title       must startWith(
        Messages("businessmatching.psr.number.title") + " - " + Messages("summary.businessmatching")
      )
      heading.html    must include(Messages("businessmatching.psr.number.title"))
      subHeading.html must include(Messages("summary.businessmatching"))

    }

    "have correct title for non-pre-submission mode" in new ViewFixture {

      val filledForm = formProvider().fill(BusinessAppliedForPSRNumberYes("1234"))

      def view = psr_number(filledForm, edit = true, isPreSubmission = false, isPsrDefined = true)

      doc.title       must startWith(
        Messages("businessmatching.psr.number.title.post.submission") + " - " + Messages("summary.updateinformation")
      )
      heading.html    must include(Messages("businessmatching.psr.number.title.post.submission"))
      subHeading.html must include(Messages("summary.updateinformation"))

    }

    "show errors in the correct locations for Radio Buttons" in new ViewFixture {

      val messageKey = "foo"

      val filledForm: Form[BusinessAppliedForPSRNumber] = formProvider().withError(FormError("appliedFor", messageKey))

      def view = psr_number(filledForm, edit = false)

      doc.getElementsByClass("govuk-list govuk-error-summary__list").first.text() mustBe messages(messageKey)

      doc.getElementById("appliedFor-error").text() mustBe s"Error: ${messages(messageKey)}"

    }

    "show errors in the correct locations for Input text field" in new ViewFixture {

      val messageKey = "error.required.msb.services"

      val filledForm: Form[BusinessAppliedForPSRNumber] = formProvider().withError(FormError("regNumber", messageKey))

      def view = psr_number(filledForm, edit = false)

      doc.getElementsByClass("govuk-list govuk-error-summary__list").first.text() mustBe messages(messageKey)

      doc.getElementById("regNumber-error").text() mustBe s"Error: ${messages(messageKey)}"
    }

    "hide the return to progress link" in new ViewFixture {
      val filledForm = formProvider().fill(BusinessAppliedForPSRNumberYes("1234"))

      def view = psr_number(filledForm, edit = true, showReturnLink = false)
      doc.body().text() must not include Messages("link.return.registration.progress")
    }

    "hide the Yes/No selection when editing an inputted PSR number and not in-presubmission mode" in new ViewFixture {
      val filledForm = formProvider().fill(BusinessAppliedForPSRNumberYes("1234"))

      override def view = psr_number(filledForm, edit = true, isPreSubmission = false, isPsrDefined = true)

      doc.body().text() must not include "Yes"
      doc.body().text() must not include "No"
    }

    "hide the Yes/No selection when editing an inputted PSR number and not in-presubmission mode and not in edit mode" in new ViewFixture {
      val filledForm = formProvider().fill(BusinessAppliedForPSRNumberYes("1234"))

      override def view = psr_number(filledForm, edit = false, isPreSubmission = false, isPsrDefined = true)

      doc.body().text() must not include "Yes"
      doc.body().text() must not include "No"
    }

    "show the Yes/No selection when editing an inputted PSR number and in pre-submission mode" in new ViewFixture {
      override def view = psr_number(formProvider(), edit = true, isPreSubmission = true)

      doc.body().text() must include("Yes")
      doc.body().text() must include("No")
    }

    "show the Yes/No selection when not editing" in new ViewFixture {
      override def view = psr_number(formProvider(), edit = false)

      doc.body().text() must include("Yes")
      doc.body().text() must include("No")
    }

    "have a back link in pre-submission mode" in new ViewFixture {
      def view = psr_number(formProvider(), edit = false, isPreSubmission = true)

      assert(
        doc.getElementsByClass("govuk-back-link") != null,
        "\n\nElement " + "govuk-back-link" + " was not rendered on the page.\n"
      )
    }

    "have a back link in non pre-submission mode" in new ViewFixture {
      def view = psr_number(formProvider(), edit = true, isPreSubmission = false)

      assert(
        doc.getElementsByClass("govuk-back-link") != null,
        "\n\nElement " + "govuk-back-link" + " was not rendered on the page.\n"
      )
    }
  }
}
