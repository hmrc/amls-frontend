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

package views.bankdetails

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.bankdetails.RemoveBankDetailsView

class RemoveBankDetailsViewSpec extends AmlsViewSpec with Matchers {

  lazy val removeBankDetails = inject[RemoveBankDetailsView]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "RemoveBankDetailsView" must {
    "have correct title" in new ViewFixture {

      def view = removeBankDetails(0, "AccountName")

      doc.title must startWith(
        messages("bankdetails.remove.bank.account.title") + " - " + messages("summary.bankdetails")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = removeBankDetails(0, "AccountName")

      heading.html    must be(messages("bankdetails.remove.bank.account.title"))
      subHeading.html must include(messages("summary.bankdetails"))

    }

    "have the correct message" in new ViewFixture {

      val accountName = "Main Account"
      def view        = removeBankDetails(0, accountName)

      doc.getElementsByTag("p").text() must include(messages("bankdetails.remove.bank.account.text", accountName))
    }

    behave like pageWithBackLink(removeBankDetails(0, "AccountName"))
  }
}
