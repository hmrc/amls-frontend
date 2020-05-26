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

package views.accessibility

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture


class accessibilityStatementSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "accessibilityStatement view" must {

    "have a back link" in new ViewFixture {

      def view = views.html.accessibility.accessibility_statement("")

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {


      def view = views.html.accessibility.accessibility_statement("")

      doc.title must startWith(Messages("Accessibility statement for Manage your anti-money laundering supervision"))
    }

    "have correct headings" in new ViewFixture {


      def view = views.html.accessibility.accessibility_statement("")

      heading.html must be(Messages("Accessibility statement for Manage your anti-money laundering supervision"))

    }

    "have correct content" in new ViewFixture {

      def view = views.html.accessibility.accessibility_statement("")

      html must include("Accessibility statement for Manage your anti-money laundering supervision")
      html must include("This accessibility statement explains how accessible this service is, what to do if you have difficulty using it, and how to report accessibility problems with the service.")
      html must include("This service is part of the wider GOV.UK website. There is a separate")
      html must include("accessibility statement")
      html must include("This page only contains information about the Manage your anti-money laundering supervision service, available at")
      html must include("www.tax.service.gov.uk/anti-money-laundering")
      html must include("You can use this service to apply to register under the Money Laundering Regulations, renew a registration, and manage your account.")
      html must include("You can also use this service to withdraw your application or deregister your business, if you no longer need to be registered under the Money Laundering Regulations.")
      html must include("This service is run by HM Revenue and Customs (HMRC). We want as many people as possible to be able to use this service. This means you should be able to:")
      html must include("We have also made the text in the service as simple as possible to understand.")
      html must include("has advice on making your device easier to use if you have a disability.")
      html must include("AbilityLink")
      html must include("This service is fully compliant with the")
      html must include("Web Content Accessibility Guidelines version 2.1 AA standard")
      html must include("There are no known accessibility issues within this service.")
      html must include("If you have difficulty using this service, contact us by:")
      html must include("We are always looking to improve the accessibility of this service. If you find any problems that are not listed on this page or think we are not meeting accessibility requirements,")
      html must include("report the accessibility problem")
      html must include("The Equality and Human Rights Commission (EHRC) is responsible for enforcing the Public Sector Bodies (Websites and Mobile Applications) (No. 2) Accessibility Regulations 2018 (the ‘accessibility regulations’). If you are not happy with how we respond to your complaint,")
      html must include("")
      html must include("")
      html must include("")
      html must include("")
    }
  }
}
