package controllers

import connectors.DataCacheConnector
import models.Country
import models.businessmatching.{BusinessMatching, BusinessType}
import models.businesscustomer.{Address, ReviewDetails}
import models.notifications._
import org.joda.time.{DateTime, DateTimeZone}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class NotificationsControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val testNotifications = Notification(
      status = None,
      messageType = None,
      referenceNumber = None,
      isVariation = false,
      timeReceived = new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC),
      isRead = true
    )

    val controller = new NotificationsController {
      override protected def authConnector: AuthConnector = self.authConnector
      override protected[controllers] val dataCacheConnector = mock[DataCacheConnector]
    }
  }

  "NotificationsController" must {
    "display the page with messages" in new Fixture {

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

      when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any()))
        .thenReturn(Future.successful(Some(testBusinessMatch)))

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
      val testList = List(
        testNotifications.copy(messageType = Some(APA1), isRead = false, timeReceived = new DateTime(1981, 12, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(isVariation = true, timeReceived = new DateTime(1976, 12, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(APR1), isRead = false, timeReceived = new DateTime(2016, 12, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(REJR), timeReceived = new DateTime(2001, 12, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications,
        testNotifications.copy(messageType = Some(REVR), timeReceived = new DateTime(1998, 12, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(EXPR), timeReceived = new DateTime(2017, 11, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(RPA1), isRead = false, timeReceived = new DateTime(2012, 12, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(RPV1), isRead = false, timeReceived = new DateTime(2017, 12, 1, 3, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(RPR1), isRead = false, timeReceived = new DateTime(2017, 12, 3, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(RPM1), timeReceived = new DateTime(2007, 12, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(RREM), timeReceived = new DateTime(1991, 12, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(MTRJ), timeReceived = new DateTime(1971, 12, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(MTRV), timeReceived = new DateTime(2017, 10, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(NMRJ), timeReceived = new DateTime(2003, 12, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(NMRV), timeReceived = new DateTime(2002, 12, 1, 1, 3, DateTimeZone.UTC)),
        testNotifications.copy(messageType = Some(OTHR), timeReceived = new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC))
      )

      val result = controller.getNotificationRecords(testList)

      result.head.timeReceived.isAfter(result.drop(1).head.timeReceived) mustBe true
      result.last.timeReceived.isBefore(result.init.last.timeReceived) mustBe true

    }
  }

}
