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

import forms.businessactivities.AccountantNonUKAddressFormProvider
import models.Country
import models.businessactivities.{NonUkAccountantsAddress, WhoIsYourAccountantIsUk, WhoIsYourAccountantName}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.businessactivities.AccountantNonUKAddressView

class AccountantNonUKAddressViewSpec extends AmlsViewSpec with Matchers with AutoCompleteServiceMocks {

  lazy val address = inject[AccountantNonUKAddressView]
  lazy val fp      = inject[AccountantNonUKAddressFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val line1   = "addressLine1"
  val line2   = "addressLine2"
  val line3   = "addressLine3"
  val line4   = "addressLine4"
  val country = "country"

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  val defaultName         = WhoIsYourAccountantName("accountantName", Some("tradingName"))
  val defaultIsUkTrue     = WhoIsYourAccountantIsUk(true)
  val defaultNonUkAddress = NonUkAccountantsAddress("line1", Some("line2"), None, None, Country("India", "IN"))

  "who_is_your_accountant_non_uk_address view" must {
    "have correct title" in new ViewFixture {

      def view =
        address(fp().fill(defaultNonUkAddress), true, defaultName.accountantsName, mockAutoComplete.formOptions)

      doc.title must startWith(messages("businessactivities.whoisyouraccountant.address.title"))
    }

    "have correct headings" in new ViewFixture {

      def view =
        address(fp().fill(defaultNonUkAddress), true, defaultName.accountantsName, mockAutoComplete.formOptions)

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
      (country, "error.invalid.country")
    ) foreach { case (field, error) =>
      behave like pageWithErrors(
        address(fp().withError(field, error), true, defaultName.accountantsName, mockAutoComplete.formOptions),
        if (field == "country") "location-autocomplete" else field,
        error
      )
    }

    behave like pageWithBackLink(address(fp(), false, defaultName.accountantsName, mockAutoComplete.formOptions))
  }
}
