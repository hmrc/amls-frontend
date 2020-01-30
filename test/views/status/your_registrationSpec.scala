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

package views.status

import forms.EmptyForm
import generators.AmlsReferenceNumberGenerator
import models.FeeResponse
import models.ResponseType.SubscriptionResponseType
import org.joda.time.{DateTime, DateTimeZone}
import org.jsoup.select.Elements
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import utils.{AmlsViewSpec, DateHelper}
import views.Fixture

class your_registrationSpec extends AmlsViewSpec with MustMatchers with AmlsReferenceNumberGenerator {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()

    val feeResponse = FeeResponse(
      SubscriptionResponseType,
      amlsRegistrationNumber,
      150.00,
      Some(100.0),
      None,
      300.0,
      550.0,
      Some("XA000000000000"),
      None,
      new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC)
    )

  }

  "your_registration view" must {

    val pageTitle = "Your registration - " + Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = HtmlFormat.empty,
        can_cannot_trade = HtmlFormat.empty)

      doc.title must be(pageTitle)
      heading.html must be(Messages("your.registration"))
    }

    "contain registration information" in new ViewFixture {

      def view = views.html.status.your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Html("some registration information"),
        can_cannot_trade = Html("some additional content"),
        unreadNotifications = 0)

      doc.getElementById("registration-info").html() must include("some registration information")
    }

    "contain your business information cell with right content" in new ViewFixture {

      def view = views.html.status.your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Html("some registration information"),
        can_cannot_trade = Html("some additional content"),
        unreadNotifications = 0)

      val yourBusinessCell = doc.getElementById("your-business")
      yourBusinessCell.getElementsByClass("heading-small").first().html() must include("Your business")
      yourBusinessCell.getElementById("status-submitted-business-name").html() must include("business Name")
      yourBusinessCell.getElementsMatchingOwnText("Check or update your business information")
        .attr("href") must be(controllers.routes.RegistrationProgressController.get().url)
    }

    "contain your registration status information cell with right content" in new ViewFixture {

      def view = views.html.status.your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Html("some registration information"),
        can_cannot_trade = Html("some additional content"),
        unreadNotifications = 0)

      val registrationStatusCell = doc.getElementById("registration-status")
      registrationStatusCell.getElementsByClass("heading-small").first().html() must include("Registration status")
      registrationStatusCell.html() must include("Application pending.")
      registrationStatusCell.html() must include("some additional content")
    }

    "contain your messages cell with right content" in new ViewFixture {

      def view = views.html.status.your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Html("some registration information"),
        can_cannot_trade = Html("some additional content"),
        unreadNotifications = 0)

      val messagesCell = doc.getElementById("messages")
      messagesCell.getElementsByClass("heading-small").first().html() must include("Messages")
      messagesCell.getElementsMatchingOwnText("Check your messages")
        .attr("href") must be(controllers.routes.NotificationController.getMessages().url)
      messagesCell.getElementsByClass("hmrc-notification-badge").isEmpty must be(true)
    }

    "contain your fees cell with right content" in new ViewFixture {

      def view = views.html.status.your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Html("some registration information"),
        can_cannot_trade = Html("some additional content"),
        unreadNotifications = 0)

      val feeCell = doc.getElementById("fees")
      feeCell.getElementsByClass("heading-small").first().html() must include("Fees")
      feeCell.html() must include("If you do not pay your fees within 28 days of submitting your application it will be rejected.")
      feeCell.getElementsMatchingOwnText("How to pay your fees")
        .attr("href") must be("how-to-pay")
    }


    "contain additional content elements" in new ViewFixture {
      def view = views.html.status.your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Html("some registration information"),
        can_cannot_trade = Html("some additional content"),
        unreadNotifications = 100)

      val messagesCell = doc.getElementById("messages")
      messagesCell.getElementsByClass("hmrc-notification-badge").isEmpty must be(false)
      messagesCell.getElementsByClass("hmrc-notification-badge").first().html() must include("100")

      doc.getElementsMatchingOwnText("contact HMRC")
        .attr("href") must be("https://www.gov.uk/government/organisations/hm-revenue-customs/contact/money-laundering")

      doc.getElementsMatchingOwnText("withdraw your application")
        .attr("href") must be(controllers.withdrawal.routes.WithdrawApplicationController.get().url)

      doc.getElementsMatchingOwnText("Give feedback on the service")
        .attr("href") must be("/anti-money-laundering/satisfaction-survey")

      doc.getElementsMatchingOwnText("Print this page")
        .attr("href") must be("javascript:window.print()")
    }
  }
}
