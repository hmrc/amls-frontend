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

package views.confirmation

import models.confirmation.Currency
import org.scalatest.MustMatchers
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.confirmation.ConfirmationNewView

class ConfirmationNewViewSpec extends AmlsViewSpec with MustMatchers with Fixture {

  lazy val confirmation = inject[ConfirmationNewView]
  implicit val request = FakeRequest()

  val totalAmount = Currency(BigDecimal(215))
  val referenceNumber = "XA1000000000001"
  val href = "/foo"

  val view = confirmation(Some(referenceNumber), totalAmount, href)

  "ConfirmationNewView" must {

    "display the correct title" in {
      doc.title() must startWith(messages("confirmation.header") + " - " + messages("submit.registration"))
    }

    "display the correct heading" in {
      heading.text() mustBe messages("confirmation.header")
    }

    "display the correct subtitle" in {
      subHeading.text() mustBe messages("submit.registration")
    }

    "display the correct guidance content" in {

      doc.text() must include(messages("confirmation.submission.info"))
      doc.text() must include(messages("confirmation.timelimit"))
    }

    "display the total" in {
      val total = doc.getElementById("total")

      total.text() mustBe totalAmount.toString
    }

    "display the reference number" in {
      val reference = doc.getElementById("reference")

      reference.text() mustBe referenceNumber
    }

    "display the print link" in {

      val button = doc.getElementById("confirmation-print")

      button.text() mustBe messages("link.print")
      button.attr("data-journey-click") mustBe "fee-reference:click:print"
      button.attr("href") mustBe "javascript:window.print()"
    }

    "display the continue button" in {

      val button = doc.getElementById("payfee")

      button.text() mustBe messages("button.continuetopayment")
      button.attr("href") mustBe href
    }
  }
}
