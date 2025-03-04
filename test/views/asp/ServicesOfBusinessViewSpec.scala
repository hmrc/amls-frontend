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

package views.asp

import forms.asp.ServicesOfBusinessFormProvider
import models.asp._
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.asp.ServicesOfBusinessView

class ServicesOfBusinessViewSpec extends AmlsViewSpec with Matchers {

  lazy val servicesOfBusiness = inject[ServicesOfBusinessView]
  lazy val fp                 = inject[ServicesOfBusinessFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "ServicesOfBusinessView" must {

    "have correct title" in new ViewFixture {

      def view = servicesOfBusiness(fp().fill(ServicesOfBusiness(Service.all.toSet)), true)

      doc.title must startWith(messages("asp.services.title") + " - " + messages("summary.asp"))
    }

    "have correct headings" in new ViewFixture {

      def view = servicesOfBusiness(fp().fill(ServicesOfBusiness(Service.all.toSet)), true)

      heading.html    must be(messages("asp.services.title"))
      subHeading.html must include(messages("summary.asp"))
    }

    behave like pageWithErrors(
      servicesOfBusiness(fp().withError("services", "error.required.asp.business.services"), true),
      "services",
      "error.required.asp.business.services"
    )

    behave like pageWithBackLink(servicesOfBusiness(fp(), false))
  }
}
