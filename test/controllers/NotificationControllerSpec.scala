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

package controllers

import cats.data.OptionT
import connectors.AmlsConnector
import controllers.actions.{SuccessfulAuthAction, SuccessfulAuthActionNoAmlsRefNo}
import generators.AmlsReferenceNumberGenerator
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.confirmation.Currency
import models.notifications.ContactType._
import models.notifications.{ContactType, IDType, NotificationDetails, NotificationRow}
import models.registrationdetails.RegistrationDetails
import models.status.{SubmissionDecisionRejected, SubmissionReadyForReview}
import models.{Country, ReadStatusResponse}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.{AuthEnrolmentsService, NotificationService}
import utils.{AmlsSpec, DependencyMocks, NotificationTemplateGenerator}
import views.html.notifications.YourMessagesView
import views.notifications._

import java.time.ZoneOffset.UTC
import java.time.{Instant, LocalDateTime}
import scala.concurrent.Future

class NotificationControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ScalaFutures
    with AmlsReferenceNumberGenerator {

  val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(1479730062573L), UTC)

  trait Fixture extends DependencyMocks { self =>

    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)

    val registrationDate: LocalDateTime       = LocalDateTime.now()
    val statusResponse: ReadStatusResponse    = ReadStatusResponse(
      registrationDate,
      "",
      None,
      None,
      None,
      None,
      renewalConFlag = false,
      safeId = Some("X123456789123")
    )
    val statusResponseBad: ReadStatusResponse =
      ReadStatusResponse(registrationDate, "", None, None, None, None, renewalConFlag = false, safeId = None)

    val testNotifications: NotificationRow = NotificationRow(
      status = None,
      contactType = None,
      contactNumber = None,
      variation = true,
      receivedAt = LocalDateTime.of(2017, 12, 1, 1, 3),
      isRead = false,
      amlsRegistrationNumber = amlsRegistrationNumber,
      templatePackageVersion = "v1m0",
      _id = IDType("132456")
    )

    val testList = Seq(
      testNotifications.copy(contactType = Some(ApplicationApproval), receivedAt = LocalDateTime.of(1981, 12, 1, 1, 3)),
      testNotifications.copy(variation = true, receivedAt = LocalDateTime.of(1976, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(RenewalApproval), receivedAt = LocalDateTime.of(2016, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(RejectionReasons), receivedAt = LocalDateTime.of(2001, 12, 1, 1, 3)),
      testNotifications,
      testNotifications.copy(contactType = Some(RevocationReasons), receivedAt = LocalDateTime.of(1998, 12, 1, 1, 3)),
      testNotifications
        .copy(contactType = Some(AutoExpiryOfRegistration), receivedAt = LocalDateTime.of(2017, 11, 1, 1, 3)),
      testNotifications
        .copy(contactType = Some(ReminderToPayForApplication), receivedAt = LocalDateTime.of(2012, 12, 1, 1, 3)),
      testNotifications
        .copy(contactType = Some(ReminderToPayForVariation), receivedAt = LocalDateTime.of(2017, 12, 1, 3, 3)),
      testNotifications
        .copy(contactType = Some(ReminderToPayForRenewal), receivedAt = LocalDateTime.of(2017, 12, 3, 1, 3)),
      testNotifications
        .copy(contactType = Some(ReminderToPayForManualCharges), receivedAt = LocalDateTime.of(2007, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(RenewalReminder), receivedAt = LocalDateTime.of(1991, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(MindedToReject), receivedAt = LocalDateTime.of(1971, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(MindedToRevoke), receivedAt = LocalDateTime.of(2017, 10, 1, 1, 3)),
      testNotifications
        .copy(contactType = Some(NoLongerMindedToReject), receivedAt = LocalDateTime.of(2003, 12, 1, 1, 3)),
      testNotifications
        .copy(contactType = Some(NoLongerMindedToRevoke), receivedAt = LocalDateTime.of(2002, 12, 1, 1, 3)),
      testNotifications.copy(contactType = Some(Others), receivedAt = LocalDateTime.of(2017, 12, 1, 1, 3)),
      testNotifications.copy(amlsRegistrationNumber = "anotherstring")
    )

    val mockAuthEnrolmentsService: AuthEnrolmentsService     = mock[AuthEnrolmentsService]
    val mockAmlsConnector: AmlsConnector                     = mock[AmlsConnector]
    val mockNotificationService: NotificationService         = mock[NotificationService]
    val mockBusinessMatchingService: BusinessMatchingService = mock[BusinessMatchingService]
    lazy val first: V1M0                                     = app.injector.instanceOf[V1M0]
    lazy val second: V2M0                                    = app.injector.instanceOf[V2M0]
    lazy val third: V3M0                                     = app.injector.instanceOf[V3M0]
    lazy val fourth: V4M0                                    = app.injector.instanceOf[V4M0]
    lazy val fifth: V5M0                                     = app.injector.instanceOf[V5M0]
    lazy val sixth: V6M0                                     = app.injector.instanceOf[V6M0]
    lazy val view: YourMessagesView                          = app.injector.instanceOf[YourMessagesView]
    val templateGenerator                                    = app.injector.instanceOf[NotificationTemplateGenerator]
    val controller                                           = new NotificationController(
      authEnrolmentsService = mockAuthEnrolmentsService,
      statusService = mockStatusService,
      businessMatchingService = mockBusinessMatchingService,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      amlsNotificationService = mockNotificationService,
      amlsConnector = mockAmlsConnector,
      dataCacheConnector = mockCacheConnector,
      cc = mockMcc,
      view = view,
      error = errorView,
      templateGenerator
    )

    val controllerWithFailedAuthAction = new NotificationController(
      authEnrolmentsService = mockAuthEnrolmentsService,
      statusService = mockStatusService,
      businessMatchingService = mockBusinessMatchingService,
      authAction = SuccessfulAuthActionNoAmlsRefNo,
      amlsNotificationService = mockNotificationService,
      amlsConnector = mockAmlsConnector,
      dataCacheConnector = mockCacheConnector,
      ds = commonDependencies,
      cc = mockMcc,
      view = view,
      error = errorView,
      notificationTemplateGenerator = templateGenerator
    )

    val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]
    val mockReviewDetails: ReviewDetails       = mock[ReviewDetails]
    val testBusinessName                       = "Ubunchews Accountancy Services"

    val testReviewDetails: ReviewDetails = ReviewDetails(
      testBusinessName,
      Some(BusinessType.LimitedCompany),
      Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")),
      "XE0001234567890"
    )

    val testBusinessMatch: BusinessMatching = BusinessMatching(
      reviewDetails = Some(testReviewDetails)
    )

    mockApplicationStatus(SubmissionReadyForReview)

    mockCacheFetch[BusinessMatching](Some(testBusinessMatch))

    when(mockStatusService.getReadStatus(any[Option[String]](), any())(any(), any()))
      .thenReturn(Future.successful(statusResponse))

    when(mockStatusService.getReadStatus(any[String](), any[(String, String)]())(any(), any()))
      .thenReturn(Future.successful(statusResponse))

    when(mockStatusService.getStatus(any(), any[(String, String)]())(any(), any()))
      .thenReturn(Future.successful(SubmissionDecisionRejected))

    when(mockBusinessMatchingService.getModel(any[String]()))
      .thenReturn(OptionT.liftF[Future, BusinessMatching](Future.successful(testBusinessMatch)))

    when {
      mockAmlsConnector.registrationDetails(any[(String, String)], any())(any(), any())
    } thenReturn Future.successful(RegistrationDetails(testBusinessName, isIndividual = false))
  }

  "getMessages" must {

    "respond with OK and show the YourMessagesView page when there is a valid safeId" in new Fixture {

      when(mockNotificationService.getNotifications(any(), any())(any(), any()))
        .thenReturn(Future.successful(testList))

      val result: Future[Result] = controller.getMessages()(request)

      status(result) mustBe OK
    }

    "respond with an error message when a valid safeId cannot be found  (AuthEnrolmentsService returns value)" in new Fixture {

      when(mockNotificationService.getNotifications(any(), any())(any(), any()))
        .thenReturn(Future.successful(testList))

      when(mockBusinessMatchingService.getModel(any()))
        .thenReturn(OptionT.liftF[Future, BusinessMatching](Future.successful(None)))

      when(mockStatusService.getReadStatus(any[String](), any[(String, String)]())(any(), any()))
        .thenReturn(Future.successful(statusResponseBad))

      intercept[Exception] {
        await(controller.getMessages()(request))
      }.getMessage must be("Unable to retrieve SafeID")
    }

    "respond with OK and show the your messages page when there is an invalid safeId and businessMatching is used" in new Fixture {

      when(mockNotificationService.getNotifications(any(), any())(any(), any()))
        .thenReturn(Future.successful(testList))

      when(mockStatusService.getReadStatus(any[Option[String]](), any[(String, String)]())(any(), any()))
        .thenReturn(Future.successful(statusResponseBad))

      val result: Future[Result] = controllerWithFailedAuthAction.getMessages()(request)

      status(result) mustBe OK
      contentAsString(result) must not include Messages("notifications.previousReg")
    }

    "respond with an error message when a valid safeId cannot be found (AuthEnrolmentsService doesn't return value)" in new Fixture {

      when(mockNotificationService.getNotifications(any(), any())(any(), any()))
        .thenReturn(Future.successful(testList))

      when(mockBusinessMatchingService.getModel(any[String]()))
        .thenReturn(OptionT.liftF[Future, BusinessMatching](Future.successful(None)))

      when(mockStatusService.getReadStatus(any[Option[String]](), any[(String, String)]())(any(), any()))
        .thenReturn(Future.successful(statusResponseBad))

      intercept[Exception] {
        await(controllerWithFailedAuthAction.getMessages()(request))
      }.getMessage must be("Unable to retrieve SafeID from reviewDetails")
    }

  }

  "messageDetails" must {

    "display the message view given the message id" when {

      "contactType is ApplicationAutorejectionForFailureToPay" in new Fixture {

        val notificationDetails: NotificationDetails = NotificationDetails(
          contactType = Some(ApplicationAutorejectionForFailureToPay),
          status = None,
          messageText = Some("Message Text"),
          variation = false,
          receivedAt = dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result: Future[Result] = controller.messageDetails(
          "dfgdhsjk",
          ContactType.ApplicationAutorejectionForFailureToPay,
          amlsRegistrationNumber,
          "v1m0"
        )(request)

        status(result) mustBe 200
        contentAsString(result) must include("Message Text")
      }

      "contactType is ReminderToPayForVariation" in new Fixture {

        val reminderVariationMessage: String =
          Messages("notification.reminder-to-pay-variation", Currency(1234), "ABC1234")

        val notificationDetails: NotificationDetails = NotificationDetails(
          contactType = Some(ReminderToPayForVariation),
          status = None,
          messageText = Some(reminderVariationMessage),
          variation = false,
          receivedAt = dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result: Future[Result] =
          controller.messageDetails("dfgdhsjk", ContactType.ReminderToPayForVariation, amlsRegistrationNumber, "v1m0")(
            request
          )

        status(result) mustBe 200
        contentAsString(result) must include(
          reminderVariationMessage
        )
      }

    }

    "display minded_to_revoke" when {
      "contact is MTRV" in new Fixture {

        val msgTxt                                   = "Considering revokation"
        val notificationDetails: NotificationDetails = NotificationDetails(
          contactType = Some(MindedToRevoke),
          status = None,
          messageText = Some(msgTxt),
          variation = false,
          receivedAt = dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result: Future[Result] =
          controller.messageDetails("id", ContactType.MindedToRevoke, amlsRegistrationNumber, "v1m0")(request)

        status(result) mustBe 200
        contentAsString(result) must include(msgTxt)
        contentAsString(result) must include(amlsRegistrationNumber)
        contentAsString(result) must include(testBusinessName)

      }
    }

    "display minded_to_reject" when {
      "contact is MTRJ" in new Fixture {

        val msgTxt                                   = "Considering revokation"
        val notificationDetails: NotificationDetails = NotificationDetails(
          contactType = Some(MindedToReject),
          status = None,
          messageText = Some(msgTxt),
          variation = false,
          receivedAt = dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result: Future[Result] =
          controller.messageDetails("id", ContactType.MindedToReject, amlsRegistrationNumber, "v1m0")(request)

        status(result) mustBe 200
        contentAsString(result) must include(msgTxt)
        contentAsString(result) must include(testBusinessName)

      }
    }

    "display rejection_reasons" when {
      "contact is REJR" in new Fixture {

        val msgTxt                                   = "Rejected"
        val notificationDetails: NotificationDetails = NotificationDetails(
          Some(RejectionReasons),
          None,
          Some(msgTxt),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result =
          controller.messageDetails("id", ContactType.RejectionReasons, amlsRegistrationNumber, "v1m0")(request)

        status(result) mustBe 200
        contentAsString(result) must include(msgTxt)
        contentAsString(result) must include(testBusinessName)
        contentAsString(result) must include(notificationDetails.dateReceived)

      }
    }

    "display no_longer_minded_to_reject" when {
      "contact is NMRV" in new Fixture {

        val msgTxt              = "Rejected"
        val notificationDetails = NotificationDetails(
          Some(RevocationReasons),
          None,
          Some(msgTxt),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result =
          controller.messageDetails("id", ContactType.NoLongerMindedToReject, amlsRegistrationNumber, "v1m0")(request)

        status(result) mustBe 200
        contentAsString(result) must not include msgTxt
        contentAsString(result) must include(
          "We’re no longer considering refusal and your application will continue as normal."
        )

      }
    }

    "display revocation_reasons" when {
      "contact is REVR" in new Fixture {

        val msgTxt              = "Revoked"
        val notificationDetails = NotificationDetails(
          Some(RevocationReasons),
          None,
          Some(msgTxt),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result =
          controller.messageDetails("id", ContactType.RevocationReasons, amlsRegistrationNumber, "v1m0")(request)

        status(result) mustBe 200
        contentAsString(result) must include(msgTxt)
        contentAsString(result) must include(testBusinessName)
        contentAsString(result) must include(notificationDetails.dateReceived)
        contentAsString(result) must include(amlsRegistrationNumber)

      }
    }

    "display no_long_minded_to_revoke" when {
      "contact is NMRV" in new Fixture {

        val msgTxt              = "Revoked"
        val notificationDetails = NotificationDetails(
          Some(RevocationReasons),
          None,
          Some(msgTxt),
          false,
          dateTime
        )

        when(mockNotificationService.getMessageDetails(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(notificationDetails)))

        val result =
          controller.messageDetails("id", ContactType.NoLongerMindedToRevoke, amlsRegistrationNumber, "v1m0")(request)

        status(result) mustBe 200
        contentAsString(result) must not include msgTxt
        contentAsString(result) must include(amlsRegistrationNumber)

      }
    }

    "respond with NOT_FOUND" when {
      "message details cannot be retrieved from service" in new Fixture {

        when(mockNotificationService.getMessageDetails(any(), any(), any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.messageDetails("", ContactType.MindedToReject, amlsRegistrationNumber, "v1m0")(request)

        status(result) must be(NOT_FOUND)
      }
    }

    "throw Exception" when {

      "safeId is not present in status response or BusinessMatching" in new Fixture {

        when(mockStatusService.getReadStatus(any[String](), any[(String, String)])(any(), any()))
          .thenReturn(Future.successful(statusResponse.copy(safeId = None)))

        intercept[Exception] {
          await(controller.messageDetails("", ContactType.MindedToReject, amlsRegistrationNumber, "v1m0")(request))
        }.getMessage must be("Unable to retrieve SafeID")
      }
    }
  }

}
