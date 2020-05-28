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
import play.twirl.api.HtmlFormat
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
      html must include("if you live in Northern Ireland")
      html must include("contact the Equality Advisory and Support Service (EASS)")
      html must include("Equality Commission for Northern Ireland (ECNI)")
      html must include("We provide a text relay service if you are deaf, hearing impaired or have a speech impediment.")
      html must include("We can provide a British Sign Language (BSL) interpreter, or you can arrange a visit from an HMRC advisor to help you complete the service.")
      html must include("Find out how to")
      html must include("contact us")
      html must include("HMRC is committed to making this service accessible, in accordance with the Public Sector Bodies (Websites and Mobile Applications) (No. 2) Accessibility Regulations 2018.")
      html must include("This service is fully compliant with the")
      html must include("Web Content Accessibility Guidelines version 2.1 AA standard")
      html must include("The service was built using parts that were tested by the Digital Accessibility Centre. The full service was tested by HMRC and included disabled users.")
    }

    "have correct list content" in new ViewFixture {
      def view = views.html.accessibility.accessibility_statement("")

      html must include("change colours, contrast levels and fonts")
      html must include("zoom in up to 300% without the text spilling off the screen")
      html must include("get from the start of the service to the end using just a keyboard")
      html must include("get from the start of the service to the end using speech recognition software")
      html must include("listen to the service using a screen reader (including the most recent versions of JAWS, NVDA and VoiceOver)")
      html must include("email: MLRCIT@hmrc.gov.uk")
      html must include("telephone: 0300 200 3700")
    }
  }
}
