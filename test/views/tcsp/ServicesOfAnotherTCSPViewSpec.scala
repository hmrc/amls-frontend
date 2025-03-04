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

package views.tcsp

import forms.tcsp.ServicesOfAnotherTCSPFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tcsp.ServicesOfAnotherTCSPView

class ServicesOfAnotherTCSPViewSpec extends AmlsViewSpec with Matchers {

  lazy val services_of_another_tcsp = inject[ServicesOfAnotherTCSPView]
  lazy val fp                       = inject[ServicesOfAnotherTCSPFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "ServicesOfAnotherTCSPView view" must {
    "have correct title, correct heading and subheading" in new ViewFixture {

      def view = services_of_another_tcsp(fp(), true)

      val title = messages("tcsp.servicesOfAnotherTcsp.title") + " - " + messages("summary.tcsp") + " - " +
        messages("title.amls") + " - " + messages("title.gov")
      doc.title       must be(title)
      heading.html    must be(messages("tcsp.servicesOfAnotherTcsp.title"))
      subHeading.html must include(messages("summary.tcsp"))
    }

    behave like pageWithErrors(
      services_of_another_tcsp(
        fp().withError("servicesOfAnotherTCSP", "error.required.tcsp.services.another.tcsp"),
        true
      ),
      "servicesOfAnotherTCSP",
      "error.required.tcsp.services.another.tcsp"
    )

    behave like pageWithBackLink(services_of_another_tcsp(fp(), false))
  }
}
