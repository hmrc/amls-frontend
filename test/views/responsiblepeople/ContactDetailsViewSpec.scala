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

package views.responsiblepeople

import forms.responsiblepeople.ContactDetailsFormProvider
import models.responsiblepeople.ContactDetails
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.ContactDetailsView

class ContactDetailsViewSpec extends AmlsViewSpec with Matchers {

  lazy val contact_details = inject[ContactDetailsView]
  lazy val fp              = inject[ContactDetailsFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val name = "firstName lastName"

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "ContactDetailsView" must {

    "have correct title" in new ViewFixture {

      def view = contact_details(fp().fill(ContactDetails("0987654", "email.com")), true, 1, None, name)

      doc.title must startWith(messages("responsiblepeople.contact_details.title"))
    }

    "have correct headings" in new ViewFixture {

      def view = contact_details(fp().fill(ContactDetails("0987654", "email.com")), true, 1, None, name)

      heading.html    must be(messages("responsiblepeople.contact_details.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    "show the person name in intro text" in new ViewFixture {

      def view = contact_details(fp(), true, 1, None, name)

      doc.body().text() must include(messages("responsiblepeople.contact_details.lbl", name))
    }

    behave like pageWithErrors(
      contact_details(
        fp().withError("phoneNumber", "error.invalid.rp.contact.phone.number"),
        false,
        1,
        None,
        name
      ),
      "phoneNumber",
      "error.invalid.rp.contact.phone.number"
    )

    behave like pageWithErrors(
      contact_details(
        fp().withError("emailAddress", "error.invalid.rp.contact.email"),
        false,
        1,
        None,
        name
      ),
      "emailAddress",
      "error.invalid.rp.contact.email"
    )

    behave like pageWithBackLink(contact_details(fp(), false, 1, None, name))
  }
}
