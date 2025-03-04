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

package views.declaration

import forms.declaration.WhoIsRegisteringFormProvider
import models.declaration.WhoIsRegistering
import models.responsiblepeople.ResponsiblePerson
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.declaration.WhoIsRegisteringThisRegistrationView

class WhoIsRegisteringThisRegistrationViewSpec extends AmlsViewSpec with Matchers {

  lazy val registrationView = inject[WhoIsRegisteringThisRegistrationView]
  lazy val fp               = inject[WhoIsRegisteringFormProvider]
  lazy val regForm          = fp("registration")

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "WhoIsRegisteringThisRegistrationView" must {
    "have correct title" in new ViewFixture {

      def view = registrationView(regForm.fill(WhoIsRegistering("PersonName")), Seq(ResponsiblePerson()))

      doc.title mustBe s"${messages("declaration.who.is.registering.title")} - ${messages("title.amls")} - ${messages("title.gov")}"
      heading.html must be(messages("declaration.who.is.registering.title", "submit.registration"))

      doc.getElementsContainingOwnText(messages("declaration.who.is.registering.text")).hasText must be(true)

      doc.select("input[type=radio]").size mustBe 1
    }

    behave like pageWithErrors(
      registrationView(
        regForm.withError("person", "error.required.declaration.who.is.registering"),
        Seq(ResponsiblePerson())
      ),
      "person",
      "error.required.declaration.who.is.registering"
    )

    behave like pageWithBackLink(registrationView(regForm, Seq(ResponsiblePerson())))
  }
}
