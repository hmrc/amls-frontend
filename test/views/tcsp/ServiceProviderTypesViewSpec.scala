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

import forms.tcsp.ServiceProviderTypesFormProvider
import models.tcsp.TcspTypes
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tcsp.ServiceProviderTypesView

class ServiceProviderTypesViewSpec extends AmlsViewSpec with Matchers {

  lazy val service_provider_types = inject[ServiceProviderTypesView]
  lazy val fp                     = inject[ServiceProviderTypesFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "ServiceProviderTypesView" must {

    "have correct title, heading and subheading" in new ViewFixture {

      def view = service_provider_types(fp().fill(TcspTypes(TcspTypes.all.toSet)), true)

      val title = messages("tcsp.kind.of.service.provider.title") + " - " + messages("summary.tcsp") + " - " +
        messages("title.amls") + " - " + messages("title.gov")
      doc.title must be(title)

      heading.html    must be(messages("tcsp.kind.of.service.provider.title"))
      subHeading.html must include(messages("summary.tcsp"))
    }

    behave like pageWithErrors(
      service_provider_types(fp().withError("serviceProviders", "error.required.tcsp.service.providers"), true),
      "serviceProviders",
      "error.required.tcsp.service.providers"
    )

    behave like pageWithBackLink(service_provider_types(fp(), false))
  }
}
