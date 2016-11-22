package controllers

import connectors.{AmlsNotificationConnector, DataCacheConnector}
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessType, _}
import models.notifications.ContactType._
import models.notifications.{IDType, NotificationRow}
import org.joda.time.{DateTime, DateTimeZone}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import services.AuthEnrolmentsService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.{ExecutionContext, Future}

class NotificationsControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val testNotifications = NotificationRow(
      status = None,
      contactType = None,
      contactNumber = None,
      variation = false,
      receivedAt = new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC),
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
      testNotifications.copy(contactType = Some(ReminderToPayForApplication),receivedAt = new DateTime(2012, 12, 1, 1, 3, DateTimeZone.UTC)),
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

    val controller = new NotificationsController {
      override val authConnector = self.authConnector
      override protected[controllers] val dataCacheConnector = mock[DataCacheConnector]
      override protected[controllers] val amlsNotificationConnector = mock[AmlsNotificationConnector]
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

  "NotificationsController" must {
    "display the page with messages" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any()))
        .thenReturn(Future.successful(Some(testBusinessMatch)))

      when(controller.authEnrolmentsService.amlsRegistrationNumber(any(),any(),any()))
        .thenReturn(Future.successful(Some("")))

      when(controller.amlsNotificationConnector.fetchAllByAmlsRegNo(any())(any(),any(),any()))
        .thenReturn(Future.successful(testList))

      val result = controller.getMessages()(request)
      val content = contentAsString(result)
      val document = Jsoup.parse(content)

      status(result) mustBe 200
      document.getElementsByClass("panel-indent").html() must include(testBusinessName)
      document.getElementsByTag("table").html() must include("Subject")
      document.getElementsByTag("table").html() must include("Date")
      document.getElementsByTag("table").html() must include("message-unread")
    }

    "get messages in chronological order (newest first)" in new Fixture {

      when(controller.amlsNotificationConnector.fetchAllByAmlsRegNo(any())(any(),any(),any()))
        .thenReturn(Future.successful(testList))

      val result = await(controller.getNotificationRecords("")(mock[HeaderCarrier],mock[AuthContext]))

      result.head.receivedAt.isAfter(result.drop(1).head.receivedAt) mustBe true
      result.last.receivedAt.isBefore(result.init.last.receivedAt) mustBe true

    }

    "throw an expection" when {
      "business name cannot be retrieved" in new Fixture {
        when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any()))
          .thenReturn(Future.successful(None))

        when(controller.authEnrolmentsService.amlsRegistrationNumber(any(),any(),any()))
          .thenReturn(Future.successful(Some("")))

        when(controller.amlsNotificationConnector.fetchAllByAmlsRegNo(any())(any(),any(),any()))
          .thenReturn(Future.successful(testList))

        val result = intercept[Exception] {
          await(controller.getMessages()(request))
        }

        result.getMessage mustBe "Cannot retrieve business name"

      }
      "enrolment does not exist" in new Fixture {
        when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(testBusinessMatch)))

        when(controller.authEnrolmentsService.amlsRegistrationNumber(any(),any(),any()))
          .thenReturn(Future.successful(None))

        when(controller.amlsNotificationConnector.fetchAllByAmlsRegNo(any())(any(),any(),any()))
          .thenReturn(Future.successful(testList))

        val result = intercept[Exception] {
          await(controller.getMessages()(request))
        }

        result.getMessage mustBe "amlsRegNo does not exist"
      }
    }
  }

}
