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

package controllers

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import generators.AmlsReferenceNumberGenerator
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.confirmation.Currency
import models.notifications.ContactType._
import models.notifications.{ContactType, IDType, NotificationDetails, NotificationRow}
import models.registrationdetails.RegistrationDetails
import models.status.{SubmissionDecisionRejected, SubmissionReadyForReview}
import models.{Country, ReadStatusResponse}
import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.Mode
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.{AuthEnrolmentsService, NotificationService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with AmlsReferenceNumberGenerator {

  val dateTime = new DateTime(1479730062573L, DateTimeZone.UTC)

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = authRequest

    val registrationDate = LocalDateTime.now()
    val statusResponse = ReadStatusResponse(registrationDate, "", None, None, None, None, renewalConFlag = false, safeId = Some("X123456789123"))
    val statusResponseBad = ReadStatusResponse(registrationDate, "", None, None, None, None, renewalConFlag = false, safeId = None)

    val testNotifications = NotificationRow(
      status = None,
      contactType = None,
      contactNumber = None,
      variation = true,
      receivedAt = new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC),
      false,
      amlsRegistrationNumber,
      "1",
      IDType("132456")
    )

    val testList = Seq(
      testNotifications.copy(contactType = Some(ApplicationApproval), receivedAt = new DateTime(1981, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(variation = true, receivedAt = new DateTime(1976, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(RenewalApproval), receivedAt = new DateTime(2016, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(RejectionReasons), receivedAt = new DateTime(2001, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications,
      testNotifications.copy(contactType = Some(RevocationReasons), receivedAt = new DateTime(1998, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(AutoExpiryOfRegistration), receivedAt = new DateTime(2017, 11, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(ReminderToPayForApplication), receivedAt = new DateTime(2012, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(ReminderToPayForVariation), receivedAt = new DateTime(2017, 12, 1, 3, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(ReminderToPayForRenewal), receivedAt = new DateTime(2017, 12, 3, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(ReminderToPayForManualCharges), receivedAt = new DateTime(2007, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(RenewalReminder), receivedAt = new DateTime(1991, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(MindedToReject), receivedAt = new DateTime(1971, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(MindedToRevoke), receivedAt = new DateTime(2017, 10, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(NoLongerMindedToReject), receivedAt = new DateTime(2003, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(NoLongerMindedToRevoke), receivedAt = new DateTime(2002, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(contactType = Some(Others), receivedAt = new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC)),
      testNotifications.copy(amlsRegistrationNumber = "anotherstring")
    )

    val mockAuthEnrolmentsService = mock[AuthEnrolmentsService]
    val mockAmlsConnector = mock[AmlsConnector]
    val mockNotificationService = mock[NotificationService]
    val mockBusinessMatchingService = mock[BusinessMatchingService]

    lazy val defaultBuilder = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .bindings(bindModules: _*).in(Mode.Test)
      .bindings(bind[NotificationService].to(mockNotificationService))
      .overrides(bind[AuthEnrolmentsService].to(mockAuthEnrolmentsService))
      .overrides(bind[AmlsConnector].to(mockAmlsConnector))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))
      .overrides(bind[BusinessMatchingService].to(mockBusinessMatchingService))

    val builder = defaultBuilder
    lazy val app = builder.build()
    lazy val controller = app.injector.instanceOf[NotificationController]

    val mockBusinessMatching = mock[BusinessMatching]
    val mockReviewDetails = mock[ReviewDetails]
    val testBusinessName = "Ubunchews Accountancy Services"

    val testReviewDetails = ReviewDetails(
      testBusinessName,
      Some(BusinessType.LimitedCompany),
      Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")),
      "XE0001234567890"
    )

    val testBusinessMatch = BusinessMatching(
      reviewDetails = Some(testReviewDetails)
    )

    mockApplicationStatus(SubmissionReadyForReview)

    mockCacheFetch[BusinessMatching](Some(testBusinessMatch))

    when(mockStatusService.getReadStatus(any())(any(), any(), any()))
      .thenReturn(Future.successful(statusResponse))

    when(mockStatusService.getReadStatus(any(), any(), any()))
      .thenReturn(Future.successful(statusResponse))

    when(mockStatusService.getStatus(any())(any(), any(), any()))
      .thenReturn(Future.successful(SubmissionDecisionRejected))

    when(mockAuthEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
      .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

    when (mockBusinessMatchingService.getModel(any(),any(),any()))
      .thenReturn(OptionT.some[Future, BusinessMatching](testBusinessMatch))

    when {
      mockAmlsConnector.registrationDetails(any())(any(), any(), any())
    } thenReturn Future.successful(RegistrationDetails(testBusinessName, isIndividual = false))
  }

  "getMessages" must {

    "respond with OK and show the your_messages page when there is a valid safeId" in new Fixture {

      when(mockNotificationService.getNotifications(any())(any(), any()))
        .thenReturn(Future.successful(testList))

      val result = controller.getMessages()(request)

      status(result) mustBe OK
    }

    "respond with an error message when a valid safeId cannot be found  (AuthEnrolmentsService returns value)" in new Fixture {

      when(mockNotificationService.getNotifications(any())(any(), any()))
        .thenReturn(Future.successful(testList))

      when (mockBusinessMatchingService.getModel(any(),any(),any()))
        .thenReturn(OptionT.some[Future, BusinessMatching](None))

      when(mockStatusService.getReadStatus(any())(any(), any(), any()))
        .thenReturn(Future.successful(statusResponseBad))

      intercept[Exception]{
        await(controller.getMessages()(request))
      }.getMessage must be("Unable to retrieve SafeID")
    }

    "respond with OK and show the your_messages page when there is an invalid safeId and businessMatching is used (AuthEnrolmentsService doesn't return value)" in new Fixture {

      when(mockNotificationService.getNotifications(any())(any(), any()))
        .thenReturn(Future.successful(testList))

      when(mockStatusService.getReadStatus(any(), any(), any()))
        .thenReturn(Future.successful(statusResponseBad))

      when(mockAuthEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.getMessages()(request)

      status(result) mustBe OK
      contentAsString(result) must not include Messages("notifications.previousReg")
    }

    "respond with an error message when a valid safeId cannot be found (AuthEnrolmentsService doesn't return value)" in new Fixture {

      when(mockNotificationService.getNotifications(any())(any(), any()))
        .thenReturn(Future.successful(testList))

      when (mockBusinessMatchingService.getModel(any(),any(),any()))
        .thenReturn(OptionT.some[Future, BusinessMatching](None))

      when(mockAuthEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(mockStatusService.getReadStatus(any(), any(), any()))
        .thenReturn(Future.successful(statusResponseBad))

      intercept[Exception]{
        await(controller.getMessages()(request))
      }.getMessage must be("Unable to retrieve SafeID from reviewDetails")
    }

  }

  "messageDetails" must {

    "display the message view given the message id" when {

      "contactType is ApplicationAutorejectionForFailureToPay" in new Fixture {

        val notificationDetails = NotificationDetails(
          Some(ApplicationAutorejectionForFailureToPay),
          None,
          Some("Message Text"),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result = controller.messageDetails(
          "dfgdhsjk",
          ContactType.ApplicationAutorejectionForFailureToPay,
          amlsRegistrationNumber,
          "1"
        )(request)

        status(result) mustBe 200
        contentAsString(result) must include("Message Text")
      }

      "contactType is ReminderToPayForVariation" in new Fixture {

        val reminderVariationMessage = Messages("notification.reminder-to-pay-variation", Currency(1234), "ABC1234")

        val notificationDetails = NotificationDetails(
          Some(ReminderToPayForVariation),
          None,
          Some(reminderVariationMessage),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result = controller.messageDetails("dfgdhsjk", ContactType.ReminderToPayForVariation, amlsRegistrationNumber, "1")(request)

        status(result) mustBe 200
        contentAsString(result) must include(
          reminderVariationMessage
        )
      }

    }

    "display minded_to_revoke" when {
      "contact is MTRV" in new Fixture {

        val msgTxt = "Considering revokation"
        val notificationDetails = NotificationDetails(
          Some(MindedToRevoke),
          None,
          Some(msgTxt),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result = controller.messageDetails("id", ContactType.MindedToRevoke, amlsRegistrationNumber, "1")(request)

        status(result) mustBe 200
        contentAsString(result) must include(msgTxt)
        contentAsString(result) must include(amlsRegistrationNumber)
        contentAsString(result) must include(testBusinessName)

      }
    }

    "display minded_to_reject" when {
      "contact is MTRJ" in new Fixture {

        val msgTxt = "Considering revokation"
        val notificationDetails = NotificationDetails(
          Some(MindedToReject),
          None,
          Some(msgTxt),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result = controller.messageDetails("id", ContactType.MindedToReject, amlsRegistrationNumber, "1")(request)

        status(result) mustBe 200
        contentAsString(result) must include(msgTxt)
        contentAsString(result) must include(testBusinessName)

      }
    }

    "display rejection_reasons" when {
      "contact is REJR" in new Fixture {

        val msgTxt = "Rejected"
        val notificationDetails = NotificationDetails(
          Some(RejectionReasons),
          None,
          Some(msgTxt),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result = controller.messageDetails("id", ContactType.RejectionReasons, amlsRegistrationNumber, "1")(request)

        status(result) mustBe 200
        contentAsString(result) must include(msgTxt)
        contentAsString(result) must include(testBusinessName)
        contentAsString(result) must include(notificationDetails.dateReceived)

      }
    }

    "display no_longer_minded_to_reject" when {
      "contact is NMRV" in new Fixture {

        val msgTxt = "Rejected"
        val notificationDetails = NotificationDetails(
          Some(RevocationReasons),
          None,
          Some(msgTxt),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result = controller.messageDetails("id", ContactType.NoLongerMindedToReject, amlsRegistrationNumber, "1")(request)

        status(result) mustBe 200
        contentAsString(result) must not include msgTxt
        contentAsString(result) must include("We’re no longer considering refusal and your application will continue as normal.")

      }
    }

    "display revocation_reasons" when {
      "contact is REVR" in new Fixture {

        val msgTxt = "Revoked"
        val notificationDetails = NotificationDetails(
          Some(RevocationReasons),
          None,
          Some(msgTxt),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result = controller.messageDetails("id", ContactType.RevocationReasons, amlsRegistrationNumber, "1")(request)

        status(result) mustBe 200
        contentAsString(result) must include(msgTxt)
        contentAsString(result) must include(testBusinessName)
        contentAsString(result) must include(notificationDetails.dateReceived)
        contentAsString(result) must include(amlsRegistrationNumber)

      }
    }

    "display no_long_minded_to_revoke" when {
      "contact is NMRV" in new Fixture {

        val msgTxt = "Revoked"
        val notificationDetails = NotificationDetails(
          Some(RevocationReasons),
          None,
          Some(msgTxt),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result = controller.messageDetails("id", ContactType.NoLongerMindedToRevoke, amlsRegistrationNumber, "1")(request)

        status(result) mustBe 200
        contentAsString(result) must not include msgTxt
        contentAsString(result) must include(amlsRegistrationNumber)

      }
    }

    "respond with NOT_FOUND" when {
      "message details cannot be retrieved from service" in new Fixture {

        val msgTxt = "Revoked"
        val notificationDetails = NotificationDetails(
          Some(RevocationReasons),
          None,
          Some(msgTxt),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.messageDetails("", ContactType.MindedToReject, amlsRegistrationNumber, "1")(request)

        status(result) must be(NOT_FOUND)
      }
    }

    "throw Exception" when {

      "safeId is not present in status response or BusinessMatching" in new Fixture {

        when(mockStatusService.getReadStatus(any())(any(), any(), any()))
          .thenReturn(Future.successful(statusResponse.copy(safeId = None)))

        intercept[Exception]{
          await(controller.messageDetails("", ContactType.MindedToReject, amlsRegistrationNumber, "1")(request))
        }.getMessage must be("Unable to retrieve SafeID")

      }
    }

  }

}
