/*
 * Copyright 2023 HM Revenue & Customs
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

/*
 * Copyright 2018 HM Revenue & Customs
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

package views.hvd

import forms.hvd.ReceiveCashPaymentsFormProvider
import org.scalatest.MustMatchers
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.hvd.ReceiveCashView

class ReceiveCashViewSpec extends AmlsViewSpec with MustMatchers  {

  lazy val cashView = inject[ReceiveCashView]
  lazy val fp = inject[ReceiveCashPaymentsFormProvider]

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "ReceiveCashView" must {

    "have correct title" in new ViewFixture {

      def view = cashView(fp().fill(true), true)

      doc.title must startWith (messages("hvd.receiving.title") + " - " + messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      def view = cashView(fp().fill(true), true)

      heading.html must be (messages("hvd.receiving.title"))
      subHeading.html must include (messages("summary.hvd"))

    }

    behave like pageWithErrors(
      cashView(fp().withError("receivePayments", "error.required.hvd.receive.cash.payments"), false),
      "receivePayments", "error.required.hvd.receive.cash.payments"
    )

    behave like pageWithBackLink(cashView(fp(), false))
  }
}
