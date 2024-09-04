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

package views.status

import generators.AmlsReferenceNumberGenerator
import models.FeeResponse
import models.ResponseType.SubscriptionResponseType
import models.status._
import org.jsoup.nodes.{Document, Element}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Call, Request}
import play.twirl.api.{Html, HtmlFormat}
import utils.{AmlsViewSpec, DateHelper}
import views.Fixture
import views.html.status.YourRegistrationView
import views.html.status.components._

import java.time.{LocalDate, LocalDateTime}

class YourRegistrationViewSpec extends AmlsViewSpec with Matchers with AmlsReferenceNumberGenerator {

  trait ViewFixture extends Fixture {
    lazy val your_registration = inject[YourRegistrationView]
    lazy val fee_information = inject[FeeInformation]
    lazy val registration_status = inject[RegistrationStatus]
    lazy val application_incomplete = inject[ApplicationIncomplete]
    lazy val application_withdrawn = inject[ApplicationWithdrawn]
    lazy val application_rejected = inject[ApplicationRejected]
    lazy val application_revoked = inject[ApplicationRevoked]
    lazy val application_expired = inject[ApplicationExpired]
    lazy val application_deregistered = inject[ApplicationDeregistered]
    lazy val application_submission_ready = inject[ApplicationSubmissionReady]
    lazy val application_renewal_submission_ready = inject[ApplicationRenewalSubmissionReady]
    lazy val application_renewal_incomplete = inject[ApplicationRenewalIncomplete]
    lazy val application_renewal_due = inject[ApplicationRenewalDue]
    lazy val application_renewal_submitted = inject[ApplicationRenewalSubmitted]
    lazy val withdrawOrDeregisterInformation = inject[WithdrawOrDeregisterInformation]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

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
      LocalDateTime.of(2017, 12, 1, 1, 3)
    )

  }

  "YourRegistrationView" must {

    val pageTitle = "Your registration - " + messages("title.amls") + " - " + messages("title.gov")

    def assertFeedbackSectionExists(doc: Document, expectedUrl: String) = {
      val feedbackSection: Element = doc.getElementById("feedback-section")

      feedbackSection.text() must include(messages("feedback.title"))
      feedbackSection.text() must include(messages("feedback.p1"))
      feedbackSection.text() must include(messages("feedback.link"))
      feedbackSection.text() must include(messages("feedback.p2"))

      feedbackSection.getElementsByTag("a").first().attr("href") mustBe expectedUrl
    }

    "have correct title, heading and sub heading" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = None,
        registrationStatus = HtmlFormat.empty,
        feeInformation = None)

      doc.title must be(pageTitle)
      heading.html must be(messages("your.registration"))
    }

    "contain registration information" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Some(Html("some registration information")),
        registrationStatus = HtmlFormat.empty,
        feeInformation = None)

      doc.getElementById("registration-info").html() must include("some registration information")
    }

    "contain registration information for status NotCompleted" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Some(application_incomplete(Some("business Name"))),
        registrationStatus = HtmlFormat.empty,
        feeInformation = None)

      doc.getElementById("incomplete-description").html() must be("Your application to register with HMRC is incomplete. You have 28 days to complete your application from when you last saved your progress.")
      doc.getElementById("return-to-saved-application").html() must include("Return to your application")
      doc.getElementById("return-to-saved-application").nextElementSibling().html() must include("for business Name")
      doc.getElementById("return-to-saved-application").attr("href") must be(controllers.routes.RegistrationProgressController.get().url)
    }

    "contain correct content for status SubmissionWithdrawn" in new ViewFixture {
      def view = your_registration("",
        Some("business Name"),
        displayCheckOrUpdateLink = false,
        yourRegistrationInfo = Some(application_withdrawn(Some("business Name"))),
        registrationStatus = registration_status(status = SubmissionWithdrawn),
        unreadNotifications = 10,
        feeInformation = None)

      doc.getElementById("application-withdrawn-description-1").text() must be("You have withdrawn your application to register with HMRC.")
      doc.getElementById("application-withdrawn-description-2").text() must be("If the Money Laundering Regulations apply to your business, you need to be registered with an appropriate supervisory body.")
      doc.getElementById("application-withdrawn-description-3").text() must be("If you’re not registered and carry out activities covered by the Money Laundering Regulations, you may be subject to civil sanctions or criminal proceedings.")
      Option(doc.getElementById("update-information")) must be(None)
      doc.getElementById("registration-status").html() must include("Not supervised. Application withdrawn.")
      doc.getElementById("new.application.button").html() must include("Start a new application")

    }

    "contain correct content for status SubmissionDecisionRejected" in new ViewFixture {
      def view = your_registration("",
        Some("business Name"),
        displayCheckOrUpdateLink = false,
        yourRegistrationInfo = Some(application_rejected(Some("business Name"))),
        registrationStatus = registration_status(status = SubmissionDecisionRejected),
        unreadNotifications = 10,
        feeInformation = None)

      doc.getElementById("application-rejected-description-1").text() must be("Your application to register with HMRC has been rejected.")
      doc.getElementById("application-rejected-description-2").text() must be("Your business is not registered with HMRC. Your business must not carry out activities that are covered by the Money Laundering Regulations.")
      Option(doc.getElementById("update-information")) must be(None)
      doc.getElementById("registration-status").html() must include("Not supervised. Application rejected.")
      doc.getElementById("new.application.button").html() must include("Start a new application")
    }

    "contain correct content for status SubmissionDecisionRevoked" in new ViewFixture {
      def view = your_registration("",
        Some("business Name"),
        displayCheckOrUpdateLink = false,
        yourRegistrationInfo = Some(application_revoked(Some("business Name"))),
        registrationStatus = registration_status(status = SubmissionDecisionRevoked),
        unreadNotifications = 10,
        feeInformation = None)

      doc.getElementById("application-revoked-description-1").text() must be("Your registration has been revoked.")
      doc.getElementById("application-revoked-description-2").text() must be("Your business is not registered with HMRC. Your business must not carry out activities that are covered by the Money Laundering Regulations.")
      Option(doc.getElementById("update-information")) must be(None)
      doc.getElementById("registration-status").html() must include("Not supervised. Registration revoked.")
      doc.getElementById("new.application.button").html() must include("Start a new application")
    }

    "contain correct content for status SubmissionDecisionExpired" in new ViewFixture {
      def view = your_registration("",
        Some("business Name"),
        displayCheckOrUpdateLink = false,
        yourRegistrationInfo = Some(application_expired(Some("business Name"))),
        registrationStatus = registration_status(status = SubmissionDecisionExpired),
        unreadNotifications = 10,
        feeInformation = None)

      doc.getElementById("application-expired-description-1").text() must be("Your registration has expired.")
      doc.getElementById("application-expired-description-2").text() must be("Your business is not registered with HMRC under the Money Laundering Regulations.")
      Option(doc.getElementById("update-information")) must be(None)
      doc.getElementById("registration-status").html() must include("Not supervised. Registration expired.")
      doc.getElementById("new.application.button").html() must include("Start a new application")
    }

    "contain correct content for status DeRegistered" in new ViewFixture {
      val deregistrationDate = Some(LocalDate.now())
      def view = your_registration("",
        Some("business Name"),
        displayCheckOrUpdateLink = false,
        yourRegistrationInfo = Some(application_deregistered(Some("business Name"))),
        registrationStatus = registration_status(status = DeRegistered, endDate = deregistrationDate),
        unreadNotifications = 10,
        feeInformation = None)

      doc.getElementById("application-deregistered-description-1").text() must be("You have deregistered your business.")
      doc.getElementById("application-deregistered-description-2").text() must be("If the Money Laundering Regulations apply to your business, you need to be registered with an appropriate supervisory body.")
      doc.getElementById("application-deregistered-description-3").text() must be("If you’re not registered and carry out activities covered by the Money Laundering Regulations, you may be subject to civil sanctions or criminal proceedings.")
      Option(doc.getElementById("update-information")) must be(None)
      doc.getElementById("registration-status").html() must include("Not supervised. Deregistered on " + DateHelper.formatDate(deregistrationDate.value) + ".")
      doc.getElementById("new.application.button").html() must include("Start a new application")

    }

    "contain registration information for status SubmissionReady" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Some(application_submission_ready(Call("GET", "/some/url"), Some("business Name"))),
        registrationStatus = HtmlFormat.empty,
        feeInformation = None)

      doc.getElementById("registration-info").html() must include("Your application to register with HMRC is ready to submit. You have 28 days to submit your application from when you last saved your progress.")
      doc.getElementById("status-submit").html() must include("Check and submit your application")
      doc.getElementById("status-submit").nextElementSibling().html() must include("for business Name")
      doc.getElementById("status-submit").attr("href") must be("/some/url")
    }

    "contain correct content for renewal statuses" when {
      "renewal section is complete" in new ViewFixture {
        val renewalDate = Some(LocalDate.now())
        val businessName = "business Name"

        def view = your_registration(amlsRegistrationNumber,
          Some(businessName),
          yourRegistrationInfo = Some(application_renewal_submission_ready(Some(businessName))),
          unreadNotifications = 10,
          registrationStatus = registration_status(Some(amlsRegistrationNumber), ReadyForRenewal(renewalDate), endDate = renewalDate),
          feeInformation = Some(fee_information(ReadyForRenewal(renewalDate))))

        doc.getElementById("registration-info").html() must include("You have completed your renewal but have not submitted it. If you do not submit your renewal, your registration with HMRC will expire.")
        doc.getElementById("go-to-about-your-business").html() must include("Check business information and submit renewal")
        doc.getElementById("go-to-about-your-business").nextElementSibling().html() must include("for business Name")
        doc.getElementById("go-to-about-your-business").attr("href") must be(controllers.routes.RegistrationProgressController.get().url)
      }

      "renewal section is incomplete" in new ViewFixture {
        val renewalDate = Some(LocalDate.now())
        val businessName = "business Name"

        def view = your_registration(amlsRegistrationNumber,
          Some(businessName),
          yourRegistrationInfo = Some(application_renewal_incomplete(Some(businessName))),
          unreadNotifications = 10,
          registrationStatus = registration_status(Some(amlsRegistrationNumber), ReadyForRenewal(renewalDate), endDate = renewalDate),
          feeInformation = Some(fee_information(ReadyForRenewal(renewalDate))))

        doc.getElementById("registration-info").html() must include("You have started your renewal but have not completed it. If you do not submit your renewal, your registration with HMRC will expire.")
        doc.getElementById("continue-renewal").html() must include("Return to your renewal")
        doc.getElementById("continue-renewal").nextElementSibling().html() must include("for business Name")
        doc.getElementById("continue-renewal").attr("href") must be(controllers.renewal.routes.WhatYouNeedController.get.url)
      }

      "renewal section is not started (due)" in new ViewFixture {
        val renewalDate = Some(LocalDate.now())
        val businessName = "business Name"

        def view = your_registration(amlsRegistrationNumber,
          Some(businessName),
          yourRegistrationInfo = Some(application_renewal_due(Some(businessName), renewalDate)),
          unreadNotifications = 10,
          registrationStatus = registration_status(Some(amlsRegistrationNumber), ReadyForRenewal(renewalDate), endDate = renewalDate),
          feeInformation = Some(fee_information(ReadyForRenewal(renewalDate))))

        doc.getElementById("registration-info").html() must include("Your registration with HMRC will expire on " + DateHelper.formatDate(renewalDate.get) + " unless you renew.")
        doc.getElementById("start-renewal").html() must include("Start your renewal")
        doc.getElementById("start-renewal").nextElementSibling().html() must include("for business Name")
        doc.getElementById("start-renewal").attr("href") must be(controllers.renewal.routes.WhatYouNeedController.get.url)
      }

      "renewal is submitted" in new ViewFixture {
        val renewalDate = Some(LocalDate.now())
        val businessName = "business Name"

        def view = your_registration(amlsRegistrationNumber,
          Some(businessName),
          yourRegistrationInfo = Some(application_renewal_submitted()),
          unreadNotifications = 10,
          registrationStatus = registration_status(Some(amlsRegistrationNumber), RenewalSubmitted(renewalDate), endDate = renewalDate),
          feeInformation = Some(fee_information(RenewalSubmitted(renewalDate))))

        doc.getElementById("renewal-pending-description-1").text() must be("You have submitted your renewal. HMRC will review your renewal after we have received payment.")
        doc.getElementById("renewal-pending-description-2").text() must be("We’ll email you when we have made a decision.")
        doc.getElementById("registration-status").html() must include("Supervised. Renewal submitted.")
        doc.getElementById("registration-status").html() must include(s"Registration number $amlsRegistrationNumber.")
        doc.getElementById("fees").getElementsMatchingOwnText("How to pay your fees").attr("href") must be("how-to-pay")
      }
    }

    "contain your business information cell with right content" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Some(Html("some registration information")),
        registrationStatus = HtmlFormat.empty,
        feeInformation = None)

      val yourBusinessCell = doc.getElementById("your-business")
      yourBusinessCell.getElementsByClass("govuk-heading-m").first().html() must include("Your business")
      yourBusinessCell.getElementById("business-name").html() must include("business Name")
      yourBusinessCell.getElementsMatchingOwnText("Check or update your business information")
        .attr("href") must be(controllers.routes.RegistrationProgressController.get().url)
    }

    "contain your registration status information cell with right content for status SubmissionReadyForReview" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Some(Html("some registration information")),
        registrationStatus = registration_status(status = SubmissionReadyForReview, canOrCannotTradeInformation = Html("some additional content")),
        feeInformation = None)

      val registrationStatusCell = doc.getElementById("registration-status")
      registrationStatusCell.getElementsByClass("govuk-heading-m").first().html() must include("Registration status")
      registrationStatusCell.html() must include("Application pending.")
      registrationStatusCell.html() must include("some additional content")
    }

    "contain your registration status information cell with right content for status SubmissionDecisionApproved" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Some(Html("some registration information")),
        registrationStatus = registration_status(status = SubmissionDecisionApproved, amlsRegNo = Some("XBML0987654345"), endDate = Some(LocalDate.now())),
        feeInformation = None)

      val registrationStatusCell = doc.getElementById("registration-status")
      registrationStatusCell.getElementsByClass("govuk-heading-m").first().html() must include("Registration status")
      registrationStatusCell.html() must include("Supervised to")
      registrationStatusCell.html() must include(DateHelper.formatDate(LocalDate.now()))
      registrationStatusCell.html() must include("Registration number XBML0987654345")
    }

    "contain your registration status information cell with right content for status SubmissionDecisionApproved if endDate is not available" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Some(Html("some registration information")),
        registrationStatus = registration_status(status = SubmissionDecisionApproved, amlsRegNo = Some("XBML0987654345"), endDate = None),
        feeInformation = None)

      val registrationStatusCell = doc.getElementById("registration-status")
      registrationStatusCell.getElementsByClass("govuk-heading-m").first().html() must include("Registration status")
      registrationStatusCell.html() must include("Supervised.")
      registrationStatusCell.html() must include("Registration number XBML0987654345")
    }

    "contain your registration status information cell with right content for status SubmissionReady" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        None,
        yourRegistrationInfo = Some(Html("some registration information")),
        registrationStatus = registration_status(status = SubmissionReady),
        feeInformation = None)

      val registrationStatusCell = doc.getElementById("registration-status")
      registrationStatusCell.getElementsByClass("govuk-heading-m").first().html() must include("Registration status")
      registrationStatusCell.html() must include("Application not submitted.")
    }

    "contain your registration status information cell with right content for status NotCompleted" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        None,
        yourRegistrationInfo = Some(Html("some registration information")),
        registrationStatus = registration_status(status = NotCompleted),
        feeInformation = None)

      val registrationStatusCell = doc.getElementById("registration-status")
      registrationStatusCell.getElementsByClass("govuk-heading-m").first().html() must include("Registration status")
      registrationStatusCell.html() must include("Application incomplete.")
    }

    "contain your messages cell with right content" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Some(Html("some registration information")),
        registrationStatus = HtmlFormat.empty,
        feeInformation = None)

      val messagesCell = doc.getElementById("messages")
      messagesCell.getElementsByClass("govuk-heading-m").first().html() must include("Messages")
      messagesCell.getElementsMatchingOwnText("Check your messages")
        .attr("href") must be(controllers.routes.NotificationController.getMessages().url)
      messagesCell.getElementsByClass("hmrc-notification-badge").isEmpty must be(true)
    }

    "contain your fees cell with right content for status SubmissionReadyForReview" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Some(Html("some registration information")),
        registrationStatus = HtmlFormat.empty,
        feeInformation = Some(fee_information(SubmissionReadyForReview)))

      val feeCell = doc.getElementById("fees")
      feeCell.getElementsByClass("govuk-heading-s").first().html() must include("Fees")
      feeCell.html() must include("If you do not pay your fees within 28 days of submitting your application it will be rejected.")
      feeCell.getElementsMatchingOwnText("How to pay your fees")
        .attr("href") must be("how-to-pay")
    }

    "contain your fees cell with right content for status SubmissionDecisionApproved" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = Some(Html("some registration information")),
        registrationStatus = HtmlFormat.empty,
        feeInformation = Some(fee_information(SubmissionDecisionApproved)))

      val feeCell = doc.getElementById("fees")
      feeCell.getElementsByClass("govuk-heading-s").first().html() must include("Fees")
      feeCell.getElementsMatchingOwnText("How to pay")
        .attr("href") must be("how-to-pay")
    }

    "contain additional content elements" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = None,
        registrationStatus = HtmlFormat.empty,
        feeInformation = None,
        unreadNotifications = 100,
        displayContactLink = true)

      val contactUrl = "/bar"

      when(appConfig.contactHmrcLink) thenReturn contactUrl

      val messagesCell = doc.getElementById("messages")
      messagesCell.getElementsByClass("hmrc-notification-badge").isEmpty must be(false)
      messagesCell.getElementsByClass("hmrc-notification-badge").first().html() must include("100")

      doc.getElementsMatchingOwnText("contact HMRC").attr("href") must be(contactUrl)

      doc.getElementsMatchingOwnText("Print this page")
        .attr("href") must be("javascript:window.print()")
    }

    "contain withdraw application link for status SubmissionReadyForReview" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = None,
        registrationStatus = HtmlFormat.empty,
        feeInformation = None,
        unreadNotifications = 100,
        withdrawOrDeregisterInformation = withdrawOrDeregisterInformation(SubmissionReadyForReview))

      doc.getElementsMatchingOwnText("withdraw your application")
        .attr("href") must be(controllers.withdrawal.routes.WithdrawApplicationController.get().url)
    }

    "contain deregister link for status SubmissionDecisionApproved" in new ViewFixture {

      def view = your_registration(amlsRegistrationNumber,
        Some("business Name"),
        Some(feeResponse),
        yourRegistrationInfo = None,
        registrationStatus = HtmlFormat.empty,
        feeInformation = None,
        unreadNotifications = 100,
        withdrawOrDeregisterInformation = withdrawOrDeregisterInformation(SubmissionDecisionApproved))

      doc.getElementsMatchingOwnText("deregister your business")
        .attr("href") must be(controllers.deregister.routes.DeRegisterApplicationController.get().url)
    }
  }
}
