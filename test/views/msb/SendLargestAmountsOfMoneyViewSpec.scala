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

import forms.msb.SendLargestAmountsFormProvider
import models.Country
import models.moneyservicebusiness.SendTheLargestAmountsOfMoney
import org.scalatest.MustMatchers
import play.api.test.FakeRequest
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.msb.SendLargestAmountsOfMoneyView

class SendLargestAmountsOfMoneyViewSpec extends AmlsViewSpec with MustMatchers with AutoCompleteServiceMocks {

  lazy val moneyView = inject[SendLargestAmountsOfMoneyView]
  lazy val fp = inject[SendLargestAmountsFormProvider]

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "SendLargestAmountsOfMoneyView view" must {

    "have correct title" in new ViewFixture {

      def view = moneyView(
        fp().fill(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))), true, mockAutoComplete.formOptions
      )

      doc.title must be(messages("msb.send.the.largest.amounts.of.money.title") +
        " - " + messages("summary.msb") +
        " - " + messages("title.amls") +
        " - " + messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = moneyView(
        fp().fill(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))), true, mockAutoComplete.formOptions
      )

      heading.html must be(messages("msb.send.the.largest.amounts.of.money.title"))
      subHeading.html must include(messages("summary.msb"))
    }

    behave like pageWithErrors(
      moneyView(
        fp().withError("largestAmountsOfMoney[0]", "error.invalid.countries.msb.sendlargestamount.country"),
        edit = true,
        mockAutoComplete.formOptions
      ),
      "location-autocomplete-0",
      "error.invalid.countries.msb.sendlargestamount.country"
    )

    behave like pageWithBackLink(moneyView(fp(), false, mockAutoComplete.formOptions))
  }
}