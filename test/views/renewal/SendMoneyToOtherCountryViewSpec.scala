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

import forms.renewal.SendMoneyToOtherCountryFormProvider
import models.renewal.SendMoneyToOtherCountry
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.SendMoneyToOtherCountryView

class SendMoneyToOtherCountryViewSpec extends AmlsViewSpec with Matchers {

  lazy val send_money_to_other_country                           = inject[SendMoneyToOtherCountryView]
  lazy val fp                                                    = inject[SendMoneyToOtherCountryFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  trait ViewFixture extends Fixture

  "SendMoneyToOtherCountryView" must {
    "have correct title" in new ViewFixture {

      def view = send_money_to_other_country(fp().fill(SendMoneyToOtherCountry(true)), true)

      doc.title must be(
        messages("renewal.msb.send.money.title") +
          " - " + messages("summary.renewal") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = send_money_to_other_country(fp().fill(SendMoneyToOtherCountry(true)), true)

      heading.html    must be(messages("renewal.msb.send.money.title"))
      subHeading.html must include(messages("summary.renewal"))
    }

    behave like pageWithErrors(
      send_money_to_other_country(fp().withError("money", "error.required.renewal.send.money"), false),
      "money",
      "error.required.renewal.send.money"
    )

    "show the correct return progress link" in new ViewFixture {

      def view = send_money_to_other_country(fp().fill(SendMoneyToOtherCountry(true)), true)
      val link = doc.getElementById("return-to-application")

      link.text()       must include(messages("link.return.renewal.progress"))
      link.attr("href") must include(controllers.renewal.routes.RenewalProgressController.get.url)
    }

    behave like pageWithBackLink(send_money_to_other_country(fp(), false))
  }
}
