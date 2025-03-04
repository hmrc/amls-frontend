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

package views.responsiblepeople.address

import forms.responsiblepeople.address.NewHomeAddressDateOfChangeFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.address.NewHomeDateOfChangeView

class NewHomeDateOfChangeViewSpec extends AmlsViewSpec with Matchers {

  lazy val dateView = inject[NewHomeDateOfChangeView]
  lazy val fp       = inject[NewHomeAddressDateOfChangeFormProvider]

  val name = "firstName lastName"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "NewHomeDateOfChangeView view" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val pageTitle = messages("responsiblepeople.new.home.date.of.change.title") + " - " +
        messages("summary.responsiblepeople") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      def view = dateView(fp(), 1, name)

      doc.title       must be(pageTitle)
      heading.html    must be(messages("responsiblepeople.new.home.date.of.change.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsContainingOwnText(messages("lbl.day")).hasText   must be(true)
      doc.getElementsContainingOwnText(messages("lbl.month")).hasText must be(true)
      doc.getElementsContainingOwnText(messages("lbl.year")).hasText  must be(true)
    }

    behave like pageWithErrors(
      dateView(fp().withError("dateOfChange", "new.home.error.required.date.fake"), 1, name),
      "dateOfChange",
      "new.home.error.required.date.fake"
    )

    behave like pageWithBackLink(
      dateView(fp(), 1, name)
    )
  }
}
