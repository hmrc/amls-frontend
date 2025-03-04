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

package views

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.html.Start

class StartViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture {
    lazy val start: Start                                          = app.injector.instanceOf[Start]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "Landing Page View" must {

    "have the correct title" in new ViewFixture {
      def view: HtmlFormat.Appendable = start()

      doc.title must startWith("Manage your anti-money laundering supervision")
    }

    "have the correct Headings" in new ViewFixture {
      def view: HtmlFormat.Appendable = start()

      heading.html must be("Manage your anti-money laundering supervision")
    }

    "contain the expected content elements" in new ViewFixture {
      def view: HtmlFormat.Appendable = start()

      html must include("Use this service to:")
      html must include("register with HMRC under the Money Laundering Regulations")
      html must include("check or update your business information")
      html must include("check messages about your registration")
      html must include("apply for annual supervision")
      html must include("deregister, if you no longer need to be registered under the Money Laundering Regulations")
      html must include(
        "You’ll need to sign in using your existing sign in details for this service. You can create new sign in details when you first register."
      )

      doc.getElementById("button").text() mustBe "Sign in"
      doc.getElementsByClass("govuk-heading-m").text() mustBe "Before you start"

      html must include(
        "Before applying to register, <a class=\"govuk-link\" href=\"https://www.gov.uk/topic/business-tax/money-laundering-regulations\">check the Money Laundering Regulations guidance from Gov.uk (opens in new tab)</a>."
      )
      html must include(
        "You need to check <a class=\"govuk-link\" href=\"https://www.gov.uk/guidance/money-laundering-regulations-registration-fees\">fees for anti-money laundering supervision (opens in new tab)</a>."
      )
      html must include("You must sign out when leaving the service.")
    }

    "contain the expected links" in new ViewFixture {
      def view: HtmlFormat.Appendable = start()

      doc
        .getElementsContainingOwnText("check the Money Laundering Regulations guidance from Gov.uk (opens in new tab)")
        .first()
        .attr("href") mustBe "https://www.gov.uk/topic/business-tax/money-laundering-regulations"
      doc
        .getElementsContainingOwnText("fees for anti-money laundering supervision (opens in new tab)")
        .first()
        .attr("href") mustBe "https://www.gov.uk/guidance/money-laundering-regulations-registration-fees"
    }
  }
}
