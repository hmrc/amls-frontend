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

package views.msb

import forms.msb.BusinessUseAnIPSPFormProvider
import models.moneyservicebusiness.BusinessUseAnIPSPNo
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.msb.BusinessUseAnIPSPView

class BusinessUseAnIPSPViewSpec extends AmlsViewSpec with Matchers {

  lazy val businessView = inject[BusinessUseAnIPSPView]
  lazy val fp           = inject[BusinessUseAnIPSPFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "BusinessUseAnIPSPView view" must {

    "have correct title" in new ViewFixture {

      def view = businessView(fp().fill(BusinessUseAnIPSPNo), true)

      doc.title must be(
        messages("msb.ipsp.title") +
          " - " + messages("summary.msb") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = businessView(fp().fill(BusinessUseAnIPSPNo), true)

      heading.html    must include(messages("msb.ipsp.title"))
      subHeading.html must include(messages("summary.msb"))
    }

    pageWithErrors(
      businessView(fp().withError("useAnIPSP", "error.required.msb.ipsp"), false),
      "useAnIPSP",
      "error.required.msb.ipsp"
    )

    pageWithErrors(
      businessView(fp().withError("name", "error.required.msb.ipsp.name"), false),
      "name",
      "error.required.msb.ipsp.name"
    )

    pageWithErrors(
      businessView(fp().withError("referenceNumber", "error.invalid.mlr.number"), false),
      "referenceNumber",
      "error.invalid.mlr.number"
    )

    pageWithBackLink(businessView(fp(), false))
  }
}
