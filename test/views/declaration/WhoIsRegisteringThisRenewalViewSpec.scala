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
import views.html.declaration.WhoIsRegisteringThisRenewalView

class WhoIsRegisteringThisRenewalViewSpec extends AmlsViewSpec with Matchers {

  lazy val renewalView = inject[WhoIsRegisteringThisRenewalView]
  lazy val fp          = inject[WhoIsRegisteringFormProvider]
  lazy val renewalForm = fp("renewal")

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "WhoIsRegisteringThisRenewalView" must {
    "have correct title" in new ViewFixture {

      def view = renewalView(renewalForm.fill(WhoIsRegistering("PersonName")), Seq(ResponsiblePerson()))

      doc.title mustBe s"${messages("declaration.renewal.who.is.registering.title")} - ${messages("title.amls")} - ${messages("title.gov")}"
      heading.html    must be(messages("declaration.renewal.who.is.registering.heading"))
      subHeading.html must include(messages("summary.submit.renewal"))

      doc.getElementsContainingOwnText(messages("declaration.renewal.who.is.registering.text")).hasText must be(true)

      doc.select("input[type=radio]").size mustBe 1
    }

    behave like pageWithErrors(
      renewalView(
        renewalForm.withError("person", "error.required.declaration.who.is.declaring.this.renewal"),
        Seq(ResponsiblePerson())
      ),
      "person",
      "error.required.declaration.who.is.declaring.this.renewal"
    )

    behave like pageWithBackLink(renewalView(renewalForm, Seq(ResponsiblePerson())))
  }
}
