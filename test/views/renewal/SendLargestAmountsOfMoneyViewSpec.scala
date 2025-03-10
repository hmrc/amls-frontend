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

package views.renewal

import forms.renewal.SendLargestAmountsOfMoneyFormProvider
import models.Country
import models.renewal.SendTheLargestAmountsOfMoney
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.renewal.SendLargestAmountsOfMoneyView

class SendLargestAmountsOfMoneyViewSpec extends AmlsViewSpec with Matchers with AutoCompleteServiceMocks {

  lazy val send_largest_amounts_of_money                         = inject[SendLargestAmountsOfMoneyView]
  lazy val fp                                                    = inject[SendLargestAmountsOfMoneyFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "SendLargestAmountsOfMoneyView" must {
    "have correct title" in new ViewFixture {

      def view = send_largest_amounts_of_money(
        fp().fill(SendTheLargestAmountsOfMoney(Seq(Country("Country", "US")))),
        true,
        mockAutoComplete.formOptions
      )

      doc.title must startWith(messages("renewal.msb.largest.amounts.title") + " - " + messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      def view = send_largest_amounts_of_money(
        fp().fill(SendTheLargestAmountsOfMoney(Seq(Country("Country", "US")))),
        true,
        mockAutoComplete.formOptions
      )

      heading.html    must be(messages("renewal.msb.largest.amounts.title"))
      subHeading.html must include(messages("summary.renewal"))
    }

    behave like pageWithErrors(
      send_largest_amounts_of_money(
        fp().withError("largestAmountsOfMoney", "error.invalid.countries.msb.sendlargestamount.country"),
        true,
        mockAutoComplete.formOptions
      ),
      "location-autocomplete-0",
      "error.invalid.countries.msb.sendlargestamount.country"
    )

    behave like pageWithBackLink(send_largest_amounts_of_money(fp(), false, mockAutoComplete.formOptions))
  }
}
