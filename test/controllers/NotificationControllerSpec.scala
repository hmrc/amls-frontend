package controllers

import connectors.{AmlsNotificationConnector, DataCacheConnector}
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.confirmation.Currency
import models.notifications.ContactType._
import models.notifications.{ContactType, IDType, NotificationDetails, NotificationRow}
import org.joda.time.{DateTime, DateTimeZone}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeApplication
import play.api.test.Helpers._
import play.api.{Application, Mode}
import services.{AuthEnrolmentsService, NotificationService}
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class NotificationControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  val notificationService = mock[NotificationService]

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules: _*).in(Mode.Test)
    .bindings(bind[NotificationService].to(notificationService))
    .build()

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = authRequest

    val testNotifications = NotificationRow(
      status = None,
      contactType = None,
      contactNumber = None,
      variation = false,
      receivedAt = new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC),
      false,
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
      testNotifications.copy(contactType = Some(Others), receivedAt = new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC))
    )

    val controller = new NotificationController {
      override val authConnector = self.authConnector
      override protected[controllers] val dataCacheConnector = mock[DataCacheConnector]
      override protected[controllers] val authEnrolmentsService = mock[AuthEnrolmentsService]
    }

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
  }

  "getMessages" must {

    "throw an exception" when {
      "business name cannot be retrieved" in new Fixture {
        when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("")))

        when(controller.amlsNotificationService.getNotifications(any())(any(), any()))
          .thenReturn(Future.successful(testList))

        val result = intercept[Exception] {
          await(controller.getMessages()(request))
        }

        result.getMessage mustBe "Cannot retrieve business name"

      }
      "enrolment does not exist" in new Fixture {
        when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(testBusinessMatch)))

        when(controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.amlsNotificationService.getNotifications(any())(any(), any()))
          .thenReturn(Future.successful(testList))

        val result = intercept[Exception] {
          await(controller.getMessages()(request))
        }

        result.getMessage mustBe "amlsRegNo does not exist"
      }
    }
  }

  "messageDetails" must {
    "display the message view given the message id for contactType ApplicationAutorejectionForFailureToPay" in new Fixture {
      when(controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(Some("Registration Number")))

      when(controller.amlsNotificationService.getMessageDetails(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(NotificationDetails(Some(ApplicationAutorejectionForFailureToPay), None, Some("Message Text"), false))))

      val result = controller.messageDetails("dfgdhsjk",ContactType.ApplicationAutorejectionForFailureToPay)(request)

      status(result) mustBe 200
      contentAsString(result) must include("Message Text")
    }

    "display the message view given the message id for contactType ReminderToPayForVariation" in new Fixture {
      when(controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(Some("Registration Number")))

      val reminderVariationMessage = Messages("notification.reminder-to-pay-variation",Currency(1234),"ABC1234")

      when(controller.amlsNotificationService.getMessageDetails(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(NotificationDetails(Some(ReminderToPayForVariation), None, Some(reminderVariationMessage), false))))

      val result = controller.messageDetails("dfgdhsjk",ContactType.ReminderToPayForVariation)(request)

      status(result) mustBe 200
      contentAsString(result) must include(
        reminderVariationMessage
      )
    }
  }
}

class NotificationControllerWithoutNotificationsSpec extends GenericTestHelper with MockitoSugar {

  val notificationService = mock[NotificationService]

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .bindings(bindModules: _*).in(Mode.Test)
    .bindings(bind[NotificationService].to(notificationService))
    .configure("Test.microservice.services.feature-toggle.notifications" -> false)
    .build()

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val controller = new NotificationController {
      override val authConnector = self.authConnector
      override protected[controllers] val dataCacheConnector = mock[DataCacheConnector]
      override protected[controllers] val authEnrolmentsService = mock[AuthEnrolmentsService]
    }

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
  }

  "NotificationsControllerWithoutNotificationsSpec" must {
    "respond with not found when toggle is off" when {
      "viewing a list of messages" in new Fixture {
        status(controller.getMessages()(request)) mustBe 404
      }
      "viewing an individual message" in new Fixture {
        status(controller.messageDetails("",ContactType.MindedToRevoke)(request)) mustBe 404
      }
    }
  }
}