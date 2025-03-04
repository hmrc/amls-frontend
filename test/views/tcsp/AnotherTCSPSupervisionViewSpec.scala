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

import forms.tcsp.AnotherTCSPSupervisionFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tcsp.AnotherTCSPSupervisionView

class AnotherTCSPSupervisionViewSpec extends AmlsViewSpec with Matchers {

  lazy val another_tcsp_supervision = inject[AnotherTCSPSupervisionView]
  lazy val fp                       = inject[AnotherTCSPSupervisionFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "another tcsp supervision view" must {
    "have correct title, heading amd subheading" in new ViewFixture {

      def view = another_tcsp_supervision(fp(), true)

      val title = messages("tcsp.anothertcspsupervision.title") + " - " + messages("summary.tcsp") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      doc.title       must be(title)
      heading.html    must be(messages("tcsp.anothertcspsupervision.header"))
      subHeading.html must include(messages("summary.tcsp"))
    }

    behave like pageWithErrors(
      another_tcsp_supervision(
        fp().withError("servicesOfAnotherTCSP", "error.required.tcsp.services.another.tcsp.registered"),
        false
      ),
      "servicesOfAnotherTCSP",
      "error.required.tcsp.services.another.tcsp.registered"
    )

    behave like pageWithErrors(
      another_tcsp_supervision(
        fp().withError("mlrRefNumber", "error.tcsp.services.another.tcsp.number.punctuation"),
        false
      ),
      "mlrRefNumber",
      "error.tcsp.services.another.tcsp.number.punctuation"
    )

    behave like pageWithBackLink(another_tcsp_supervision(fp(), false))
  }
}
