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

import forms.tcsp.ProvidedServicesFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.{FakeRequest, Injecting}
import utils.AmlsViewSpec
import views.Fixture
import views.html.tcsp.ProvidedServicesView

class ProvidedServicesViewSpec extends AmlsViewSpec with Matchers with Injecting {

  lazy val provided_services: ProvidedServicesView = inject[ProvidedServicesView]
  lazy val fp: ProvidedServicesFormProvider        = inject[ProvidedServicesFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "ProvidedServicesView" must {
    "have correct title, heading amd subheading" in new ViewFixture {

      def view = provided_services(fp(), true)

      val title = messages("tcsp.provided_services.title") + " - " + messages("summary.tcsp") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      doc.title       must be(title)
      heading.html    must be(messages("tcsp.provided_services.title"))
      subHeading.html must include(messages("summary.tcsp"))
    }

    behave like pageWithErrors(
      provided_services(fp().withError("services", "error.required.tcsp.provided_services.services"), true),
      "services",
      "error.required.tcsp.provided_services.services"
    )

    behave like pageWithErrors(
      provided_services(fp().withError("details", "error.required.tcsp.provided_services.details"), true),
      "details",
      "error.required.tcsp.provided_services.details"
    )

    behave like pageWithBackLink(provided_services(fp(), false))
  }
}
