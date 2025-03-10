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

import forms.businessmatching.MsbSubSectorsFormProvider
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import models.businessmatching.{BusinessMatchingMsbService, BusinessMatchingMsbServices}
import org.scalatest.matchers.must.Matchers
import play.api.data.{Form, FormError}
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.MsbServicesView

class MsbServicesViewSpec extends AmlsViewSpec with Matchers {

  lazy val services                                              = app.injector.instanceOf[MsbServicesView]
  lazy val formProvider                                          = app.injector.instanceOf[MsbSubSectorsFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  val sortedServices = BusinessMatchingMsbServices.all.sortBy(_.toString)

  "services view" must {
    "have correct title in when in presubmission mode " in new ViewFixture {

      val filledForm: Form[Seq[BusinessMatchingMsbService]] = formProvider().fill(Seq(TransmittingMoney))

      def view = services(filledForm, edit = true, isPreSubmission = true)

      doc.title       must startWith(messages("msb.services.title") + " - " + messages("summary.businessmatching"))
      heading.html    must be(messages("msb.services.title"))
      subHeading.html must include(messages("summary.businessmatching"))
    }

    "have correct title in when in non-presubmission mode " in new ViewFixture {

      val filledForm: Form[Seq[BusinessMatchingMsbService]] = formProvider().fill(Seq(TransmittingMoney))

      def view = services(filledForm, edit = true, isPreSubmission = false)

      doc.title       must startWith(messages("msb.services.title") + " - " + messages("summary.updateinformation"))
      heading.html    must be(messages("msb.services.title"))
      subHeading.html must include(messages("summary.updateinformation"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val messageKey = "error.required.msb.services"

      val filledForm: Form[Seq[BusinessMatchingMsbService]] = formProvider().withError(FormError("value", messageKey))

      def view = services(filledForm, edit = true)

      doc.getElementsByClass("govuk-list govuk-error-summary__list").first.text() mustBe messages(messageKey)

      doc.getElementById("value-error").text() mustBe s"Error: ${messages(messageKey)}"
    }

    "hide the return to progress link" in new ViewFixture {
      val filledForm: Form[Seq[BusinessMatchingMsbService]] = formProvider().fill(Seq(TransmittingMoney))

      def view = services(filledForm, edit = true, showReturnLink = false)
      doc.body().text() must not include messages("link.return.registration.progress")
    }

    "show the correct amount of checkboxes" in new ViewFixture {
      def view = services(formProvider(), edit = false)
      doc.body().getElementsByAttributeValue("type", "checkbox").size() mustEqual 4
    }

    "show the correct label for the checkboxes" in new ViewFixture {
      def view       = services(formProvider(), fxEnabledToggle = true, edit = false)
      val checkboxes = doc.body().getElementsByAttributeValue("type", "checkbox")
      (0 until checkboxes.size()) foreach { i =>
        checkboxes.get(i).`val`() mustEqual sortedServices(i).toString

      }
    }

    "show selected checkboxes as checked" in new ViewFixture {
      val filledForm: Form[Seq[BusinessMatchingMsbService]] = formProvider().fill(Seq(TransmittingMoney))
      def view                                              = services(filledForm, edit = true)
      val checkbox                                          = doc.select("input[checked]").first()
      checkbox.`val`() mustBe TransmittingMoney.toString
    }

    "have a back link in pre-submission mode" in new ViewFixture {
      def view = services(formProvider(), edit = true, isPreSubmission = true)

      assert(
        doc.getElementsByClass("govuk-back-link") != null,
        "\n\nElement " + "govuk-back-link" + " was not rendered on the page.\n"
      )
    }

    "have a back link in non pre-submission mode" in new ViewFixture {
      def view = services(formProvider(), edit = true, isPreSubmission = false)

      assert(
        doc.getElementsByClass("govuk-back-link") != null,
        "\n\nElement " + "govuk-back-link" + " was not rendered on the page.\n"
      )
    }
  }

  "services fx-enabled view" must {

    "show the correct amount of checkboxes" in new ViewFixture {
      def view = services(formProvider(), edit = false, fxEnabledToggle = true)

      doc.body().getElementsByAttributeValue("type", "checkbox").size() mustEqual 5
    }

    "show the correct label for the checkboxes" in new ViewFixture {
      def view = services(formProvider(), edit = false, fxEnabledToggle = true)

      val checkboxes = doc.body().getElementsByAttributeValue("type", "checkbox")
      (0 until checkboxes.size()) foreach { i =>
        checkboxes.get(i).attr("value") mustEqual sortedServices(i).toString
      }
    }
  }

}
