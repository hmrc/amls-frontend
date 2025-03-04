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

package views.applicationstatus

import config.ApplicationConfig
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.applicationstatus.HowToPayView

class HowToPayViewSpec extends AmlsViewSpec with Matchers {

  implicit lazy val config: ApplicationConfig                    = inject[ApplicationConfig]
  lazy val howToPay                                              = app.injector.instanceOf[HowToPayView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "HowToPayView" must {
    "Have the correct title" in new ViewFixture {
      def view = howToPay(Some("ref"))
      doc.title must startWith(messages("howtopay.title"))
    }

    "Have the correct Headings" in new ViewFixture {
      def view = howToPay(Some("ref"))

      heading.html    must be(messages("howtopay.title"))
      subHeading.html must include(messages("summary.status"))
    }

    "contain the expected content elements where there is a payment reference" in new ViewFixture {
      def view = howToPay(Some("ref"))

      html must include(messages("howtopay.title2"))
      html must include(messages("howtopay.para.1"))
      html must include(messages("howtopay.para.2"))
      html must include(messages("howtopay.para.2.link"))
      html must include(messages("howtopay.para.2.b"))
      html must include(messages("howtopay.para.3"))
      html must include(messages("howtopay.para.3.link"))

      html must include(messages("howtopay.title3"))
      html must include(messages("howtopay.para.4"))
      html must include("ref")
      html must include(messages("howtopay.para.4.a"))

      html must include(messages("howtopay.title4"))
      html must include(messages("howtopay.para.5"))
      html must include(messages("howtopay.para.5.link"))
      html must include(messages("howtopay.para.6"))
      html must include(messages("howtopay.para.6.link"))
      html must include(messages("howtopay.para.7"))

      doc.getElementById("your-messages").attr("href") must be(
        controllers.routes.NotificationController.getMessages().url
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

    "contain the expected content elements where there is not a payment reference" in new ViewFixture {
      def view = howToPay(None)

      html must include(messages("howtopay.title2"))
      html must include(messages("howtopay.para.1"))
      html must include(messages("howtopay.para.2"))
      html must include(messages("howtopay.para.2.link"))

      html must include(messages("howtopay.title3"))
      html must include(messages("howtopay.para.4.b"))
      html must include(messages("howtopay.para.4.c"))
      html must include(messages("howtopay.para.2.link"))
      html must include(messages("full.stop"))

      html must include(messages("howtopay.title3.b"))
      html must include(messages("howtopay.para.3.b"))
      html must include(messages("howtopay.para.3.link"))

      html must include(messages("howtopay.title4"))
      html must include(messages("howtopay.para.5"))
      html must include(messages("howtopay.para.5.link"))
      html must include(messages("howtopay.para.6"))
      html must include(messages("howtopay.para.6.link"))

      doc.getElementById("your-messages-no-ref").attr("href") must be(
        controllers.routes.NotificationController.getMessages().url
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

    behave like pageWithBackLink(howToPay(None))
  }
}
