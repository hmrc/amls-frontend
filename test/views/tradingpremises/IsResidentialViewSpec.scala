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

import forms.tradingpremises.IsResidentialFormProvider
import models.Country
import models.businesscustomer.Address
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.IsResidentialView

class IsResidentialViewSpec extends AmlsViewSpec with Matchers {

  lazy val is_residential = inject[IsResidentialView]
  lazy val fp             = inject[IsResidentialFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val address = Address(
    "56 Southview Road",
    Some("Newcastle Upon Tyne"),
    Some("Tyne and Wear"),
    Some("Whitehill"),
    Some("NE3 6JAX"),
    Country(
      "United Kingdom",
      "UK"
    )
  )

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "IsResidentialView" must {

    "have correct title, heading, back link and load UI with empty form" in new ViewFixture {

      val pageTitle = messages("tradingpremises.isResidential.title") + " - " +
        messages("summary.tradingpremises") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      def view = is_residential(fp(), Some(address), 1, false)

      doc.title       must be(pageTitle)
      heading.html    must be(messages("tradingpremises.isResidential.title"))
      subHeading.html must include(messages("summary.tradingpremises"))

      doc.select("input[type=radio]").size() must be(2)
    }

    behave like pageWithErrors(
      is_residential(
        fp().withError("isResidential", "tradingpremises.yourtradingpremises.isresidential.required"),
        Some(address),
        1,
        false
      ),
      "isResidential",
      "tradingpremises.yourtradingpremises.isresidential.required"
    )

    behave like pageWithBackLink(is_residential(fp(), None, 1, true))

  }
}
