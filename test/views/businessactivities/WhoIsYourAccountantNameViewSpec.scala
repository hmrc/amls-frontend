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

import forms.businessactivities.WhoIsYourAccountantNameFormProvider
import models.businessactivities.{UkAccountantsAddress, WhoIsYourAccountantIsUk, WhoIsYourAccountantName}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.businessactivities.WhoIsYourAccountantNameView

class WhoIsYourAccountantNameViewSpec extends AmlsViewSpec with Matchers {

  lazy val accountant = inject[WhoIsYourAccountantNameView]
  lazy val fp         = inject[WhoIsYourAccountantNameFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  val defaultName      = WhoIsYourAccountantName("accountantName", Some("tradingName"))
  val defaultIsUkTrue  = WhoIsYourAccountantIsUk(true)
  val defaultUkAddress = UkAccountantsAddress("line1", Some("line2"), None, None, "AB12CD")

  "who_is_your_accountant_name view" must {
    "have correct title" in new ViewFixture {

      def view = accountant(fp().fill(defaultName), true)

      doc.title must startWith(messages("businessactivities.whoisyouraccountant.title"))
    }

    "have correct headings" in new ViewFixture {

      def view = accountant(fp().fill(defaultName), true)

      heading.html    must be(messages("businessactivities.whoisyouraccountant.title"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    behave like pageWithErrors(
      accountant(fp().withError("name", "error.required.ba.advisor.name"), true),
      "name",
      "error.required.ba.advisor.name"
    )

    behave like pageWithErrors(
      accountant(fp().withError("tradingName", "error.length.ba.advisor.tradingname"), true),
      "tradingName",
      "error.length.ba.advisor.tradingname"
    )

    behave like pageWithBackLink(accountant(fp(), false))
  }
}
