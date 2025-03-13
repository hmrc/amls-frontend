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

package views.tradingpremises

import forms.tradingpremises.BusinessStructureFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.BusinessStructureView

class BusinessStructureViewSpec extends AmlsViewSpec with Matchers {

  lazy val business_structure = app.injector.instanceOf[BusinessStructureView]
  lazy val fp                 = inject[BusinessStructureFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "BusinessStructureView" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val pageTitle = messages("tradingpremises.businessStructure.title") + " - " +
        messages("summary.tradingpremises") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      def view = business_structure(fp(), 1, false)

      doc.title       must be(pageTitle)
      heading.html    must be(messages("tradingpremises.businessStructure.title"))
      subHeading.html must include(messages("summary.tradingpremises"))

      doc.select("input[type=radio]").size mustBe 5
    }

    behave like pageWithErrors(
      business_structure(
        fp().withError("agentsBusinessStructure", "error.required.tp.select.business.structure"),
        1,
        true
      ),
      "agentsBusinessStructure",
      "error.required.tp.select.business.structure"
    )

    behave like pageWithBackLink(business_structure(fp(), 1, true))
  }
}
