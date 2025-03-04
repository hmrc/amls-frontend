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

package views.confirmation

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.confirmation.PaymentConfirmationRenewalView

class PaymentConfirmedRenewalViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture {
    lazy val payment_confirmation_renewal                          = inject[PaymentConfirmationRenewalView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

    val businessName     = "Test Business Ltd"
    val paymentReference = "XMHSG000000000"

    override def view = payment_confirmation_renewal(businessName, paymentReference)

  }

  "The payment confirmation renewal view" must {

    "show the correct title" in new ViewFixture {

      doc.title must startWith(messages("confirmation.payment.renewal.title"))
    }

    "show the correct heading" in new ViewFixture {

      heading.text must be(messages("confirmation.payment.renewal.lede"))
    }

    "show the company name and reference in the heading" in new ViewFixture {

      val headingContainer = doc.select(".govuk-panel__body")

      headingContainer.text must include(businessName)
      headingContainer.text must include(messages("confirmation.payment.reference_header", paymentReference))

    }
  }
}
