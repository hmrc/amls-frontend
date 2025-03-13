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

import forms.businessdetails.PreviouslyRegisteredFormProvider
import models.businessdetails.{PreviouslyRegistered, PreviouslyRegisteredYes}
import org.scalatest.matchers.must.Matchers
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.PreviouslyRegisteredView

class PreviouslyRegisteredViewSpec extends AmlsViewSpec with Matchers {

  lazy val previously_registered                                 = app.injector.instanceOf[PreviouslyRegisteredView]
  lazy val formProvider                                          = app.injector.instanceOf[PreviouslyRegisteredFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "previously_registered view" must {
    "have correct title" in new ViewFixture {

      val form2: Form[PreviouslyRegistered] = formProvider().fill(PreviouslyRegisteredYes(Some("prevMLRRegNo")))

      def view = previously_registered(form2, true)

      doc.title must startWith(
        Messages("businessdetails.registeredformlr.title") + " - " + Messages("summary.businessdetails")
      )
    }

    "have correct headings" in new ViewFixture {

      val form2: Form[PreviouslyRegistered] = formProvider().fill(PreviouslyRegisteredYes(Some("prevMLRRegNo")))

      def view = previously_registered(form2, true)

      heading.html    must be(Messages("businessdetails.registeredformlr.title"))
      subHeading.html must include(Messages("summary.businessdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val errorKey = "error.required.atb.previously.registered"

      val form2: Form[PreviouslyRegistered] =
        formProvider().withError(FormError("value", "error.required.atb.previously.registered"))

      def view = previously_registered(form2, true)

      doc.getElementsByClass("govuk-error-summary__list").text() must include(messages(errorKey))

      doc.getElementById("value-error").text() must include(messages(errorKey))

    }

    behave like pageWithBackLink(previously_registered(formProvider(), true))

  }
}
