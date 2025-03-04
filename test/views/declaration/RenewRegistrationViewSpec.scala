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

import forms.declaration.RenewRegistrationFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.declaration.RenewRegistrationView

import java.time.LocalDate

class RenewRegistrationViewSpec extends AmlsViewSpec with Matchers {

  lazy val renewView: RenewRegistrationView  = inject[RenewRegistrationView]
  lazy val fp: RenewRegistrationFormProvider = inject[RenewRegistrationFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  val endDate = LocalDate.now().minusDays(1)

  "RenewRegistrationView" must {

    "have correct title, headings and content" in new ViewFixture {

      def view = renewView(fp(), Some(endDate))

      doc.title mustBe s"${messages("declaration.renew.registration.title")} - ${messages("summary.declaration")} - ${messages("title.amls")} - ${messages("title.gov")}"
      heading.html    must include(messages("declaration.renew.registration.title"))
      subHeading.html must include(messages("summary.declaration"))

      doc.text() must include(messages("declaration.renew.registration.section1"))
      doc.text() must include(messages("declaration.renew.registration.section2", utils.DateHelper.formatDate(endDate)))
    }

    behave like pageWithErrors(
      renewView(
        fp().withError("renewRegistration", "error.required.declaration.renew.registration"),
        Some(endDate)
      ),
      "renewRegistration",
      "error.required.declaration.renew.registration"
    )

    behave like pageWithBackLink(renewView(fp(), Some(endDate)))
  }
}
