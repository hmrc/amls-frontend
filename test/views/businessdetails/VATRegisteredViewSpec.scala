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

package views.businessdetails

import forms.businessdetails.VATRegisteredFormProvider
import models.businessdetails.VATRegisteredYes
import org.scalatest.matchers.must.Matchers
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.VATRegisteredView

class VATRegisteredViewSpec extends AmlsViewSpec with Matchers {

  lazy val vat_registered                                        = app.injector.instanceOf[VATRegisteredView]
  lazy val formProvider                                          = app.injector.instanceOf[VATRegisteredFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "vat_registered view" must {
    "have correct title" in new ViewFixture {

      val formWithData = formProvider().fill(VATRegisteredYes("1234"))

      def view = vat_registered(formWithData, true)

      doc.title must startWith(
        Messages("businessdetails.registeredforvat.title") + " - " + Messages("summary.businessdetails")
      )
    }

    "have correct headings" in new ViewFixture {

      val formWithData = formProvider().fill(VATRegisteredYes("1234"))

      def view = vat_registered(formWithData, true)

      heading.html    must be(Messages("businessdetails.registeredforvat.title"))
      subHeading.html must include(Messages("summary.businessdetails"))

    }

    "show errors in the correct locations for Radio Buttons" in new ViewFixture {

      val messageKey = "foo"

      val filledForm = formProvider().withError(FormError("registeredForVAT", messageKey))

      def view = vat_registered(filledForm, edit = false)

      doc.getElementsByClass("govuk-list govuk-error-summary__list").first.text() mustBe messages(messageKey)

      doc.getElementById("registeredForVAT-error").text() mustBe s"Error: ${messages(messageKey)}"

    }

    "show errors in the correct locations for Input text field" in new ViewFixture {

      val messageKey = "error.required.msb.services"

      val filledForm = formProvider().withError(FormError("vrnNumber", messageKey))

      def view = vat_registered(filledForm, edit = false)

      doc.getElementsByClass("govuk-list govuk-error-summary__list").first.text() mustBe messages(messageKey)

      doc.getElementById("vrnNumber-error").text() mustBe s"Error: ${messages(messageKey)}"
    }

    behave like pageWithBackLink(vat_registered(formProvider(), true))

  }
}
