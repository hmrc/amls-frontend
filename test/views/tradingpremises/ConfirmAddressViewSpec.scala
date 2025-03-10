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

import forms.tradingpremises.ConfirmAddressFormProvider
import models.Country
import models.businesscustomer.Address
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.ConfirmAddressView

class ConfirmAddressViewSpec extends AmlsViewSpec with Matchers {

  lazy val confirm_address = inject[ConfirmAddressView]
  lazy val fp              = inject[ConfirmAddressFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "ConfirmAddressView" must {
    val address =
      Address("#11", Some("some building"), Some("Some street"), Some("city"), None, Country("United Kingdom", "UK"))
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val pageTitle = messages("tradingpremises.confirmaddress.title") + " - " +
        messages("summary.tradingpremises") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      def view = confirm_address(fp(), address, 1)

      doc.title       must be(pageTitle)
      heading.html    must be(messages("tradingpremises.confirmaddress.title"))
      subHeading.html must include(messages("summary.tradingpremises"))

      doc.getElementsMatchingOwnText("#11").text mustBe "#11 some building Some street city United Kingdom"
      doc.select("input[type=radio]").size mustBe 2
    }

    behave like pageWithErrors(
      confirm_address(
        fp().withError("confirmAddress", "error.required.tp.confirm.address"),
        address,
        1
      ),
      "confirmAddress",
      "error.required.tp.confirm.address"
    )

    behave like pageWithBackLink(confirm_address(fp(), address, 1))
  }
}
