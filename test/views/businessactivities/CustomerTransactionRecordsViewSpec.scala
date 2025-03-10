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

import forms.businessactivities.TransactionRecordFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.CustomerTransactionRecordsView

class CustomerTransactionRecordsViewSpec extends AmlsViewSpec with Matchers {

  lazy val customer: CustomerTransactionRecordsView    = inject[CustomerTransactionRecordsView]
  lazy val formProvider: TransactionRecordFormProvider = inject[TransactionRecordFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "CustomerTransactionRecordsView" must {
    "have correct title" in new ViewFixture {

      def view = customer(formProvider().fill(true), true)

      doc.title must startWith(messages("businessactivities.keep.customer.records.title"))
    }

    "have correct headings" in new ViewFixture {

      def view = customer(formProvider().fill(false), true)

      heading.html    must be(messages("businessactivities.keep.customer.records.title"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    behave like pageWithErrors(
      customer(formProvider().withError("isRecorded", "error.required.ba.select.transaction.record"), true),
      "isRecorded",
      "error.required.ba.select.transaction.record"
    )

    behave like pageWithBackLink(customer(formProvider(), true))
  }
}
