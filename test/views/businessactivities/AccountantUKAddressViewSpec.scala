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

package views.businessactivities

import forms.businessactivities.AccountantUKAddressFormProvider
import models.businessactivities.{UkAccountantsAddress, WhoIsYourAccountantIsUk, WhoIsYourAccountantName}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.businessactivities.AccountantUKAddressView

class AccountantUKAddressViewSpec extends AmlsViewSpec with Matchers {

  lazy val address = inject[AccountantUKAddressView]
  lazy val fp      = inject[AccountantUKAddressFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val line1    = "addressLine1"
  val line2    = "addressLine2"
  val line3    = "addressLine3"
  val line4    = "addressLine4"
  val postCode = "postCode"

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  val defaultName      = WhoIsYourAccountantName("accountantName", Some("tradingName"))
  val defaultIsUkTrue  = WhoIsYourAccountantIsUk(true)
  val defaultUkAddress = UkAccountantsAddress("line1", Some("line2"), None, None, "AB12CD")

  "who_is_your_accountant_uk_address view" must {
    "have correct title" in new ViewFixture {

      def view = address(fp().fill(defaultUkAddress), true, defaultName.accountantsName)

      doc.title must startWith(messages("businessactivities.whoisyouraccountant.address.title"))
    }

    "have correct headings" in new ViewFixture {

      def view = address(fp().fill(defaultUkAddress), true, defaultName.accountantsName)

      heading.html    must be(
        messages("businessactivities.whoisyouraccountant.address.header", defaultName.accountantsName)
      )
      subHeading.html must include(messages("summary.businessactivities"))

    }

    Seq(
      (line1, "error.text.validation.address.line1"),
      (line2, "error.text.validation.address.line2"),
      (line3, "error.text.validation.address.line3"),
      (line4, "error.text.validation.address.line4"),
      (postCode, "error.invalid.postcode")
    ) foreach { case (field, error) =>
      behave like pageWithErrors(
        address(fp().withError(field, error), true, defaultName.accountantsName),
        field,
        error
      )
    }

    behave like pageWithBackLink(address(fp(), true, defaultName.accountantsName))
  }
}
