/*
 * Copyright 2020 HM Revenue & Customs
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

package views.applicationstatus

import forms.{EmptyForm, Form2}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class how_to_paySpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = views.html.applicationstatus.how_to_pay(Some("ref"))
      doc.title must startWith(Messages("howtopay.title"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.applicationstatus.how_to_pay(Some("ref"))

      heading.html must be (Messages("howtopay.title"))
      subHeading.html must include (Messages("summary.status"))
    }

    "contain the expected content elements where there is a payment reference" in new ViewFixture{
      def view = views.html.applicationstatus.how_to_pay(Some("ref"))

      html must include(Messages("howtopay.title2"))
      html must include(Messages("howtopay.para.1"))
      html must include(Messages("howtopay.para.2"))
      html must include(Messages("howtopay.para.2.link"))
      html must include(Messages("howtopay.para.2.b"))
      html must include(Messages("howtopay.para.3"))
      html must include(Messages("howtopay.para.3.link"))

      html must include(Messages("howtopay.title3"))
      html must include(Messages("howtopay.para.4"))
      html must include("ref")
      html must include(Messages("howtopay.para.4.a"))

      html must include(Messages("howtopay.title4"))
      html must include(Messages("howtopay.para.5"))
      html must include(Messages("howtopay.para.5.link"))
      html must include(Messages("howtopay.para.6"))
      html must include(Messages("howtopay.para.6.link"))
      html must include(Messages("howtopay.para.7"))

      doc.getElementById("your-messages").attr("href") must be(
        "your-registration/your-messages"
      )

      doc.getElementById("find-email").attr("href") must be(
        "https://www.gov.uk/guidance/money-laundering-regulations-registration-fees#how-to-pay"
      )

      doc.getElementById("card-payment").attr("href") must be(
        "https://www.gov.uk/pay-tax-debit-credit-card"
      )

      doc.getElementById("ways-to-pay").attr("href") must be(
        "https://www.gov.uk/guidance/pay-money-laundering-regulations-fees-and-penalty-charges#ways-to-pay"
      )
    }

    "contain the expected content elements where there is not a payment reference" in new ViewFixture{
      def view = views.html.applicationstatus.how_to_pay(None)

      html must include(Messages("howtopay.title2"))
      html must include(Messages("howtopay.para.1"))
      html must include(Messages("howtopay.para.2"))
      html must include(Messages("howtopay.para.2.link.stop"))

      html must include(Messages("howtopay.title3"))
      html must include(Messages("howtopay.para.4.b"))
      html must include(Messages("howtopay.para.4.c"))
      html must include(Messages("howtopay.para.2.link"))
      html must include(Messages("full.stop"))

      html must include(Messages("howtopay.title3.b"))
      html must include(Messages("howtopay.para.3.b"))
      html must include(Messages("howtopay.para.3.link"))

      html must include(Messages("howtopay.title4"))
      html must include(Messages("howtopay.para.5"))
      html must include(Messages("howtopay.para.5.link"))
      html must include(Messages("howtopay.para.6"))
      html must include(Messages("howtopay.para.6.link"))

      doc.getElementById("your-messages-no-ref").attr("href")must be(
        "your-registration/your-messages"
      )

      doc.getElementById("find-email-no-reference").attr("href") must be(
        "https://www.gov.uk/guidance/money-laundering-regulations-registration-fees#how-to-pay"
      )

      doc.getElementById("card-payment").attr("href") must be(
        "https://www.gov.uk/pay-tax-debit-credit-card"
      )

      doc.getElementById("ways-to-pay").attr("href") must be(
        "https://www.gov.uk/guidance/pay-money-laundering-regulations-fees-and-penalty-charges#ways-to-pay"
      )
    }

    "have a back link" in new ViewFixture {
      val form2: Form2[_] = EmptyForm

      def view = views.html.applicationstatus.how_to_pay(None)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}