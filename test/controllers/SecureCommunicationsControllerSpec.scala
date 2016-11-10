package controllers

import connectors.DataCacheConnector
import models.Country
import models.businessmatching.{BusinessMatching, BusinessType}
import models.businesscustomer.{Address, ReviewDetails}
import models.securecommunications._
import org.joda.time.LocalDate
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

class SecureCommunicationsControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val testSecureComms = SecureCommunication(
      status = None,
      messageType = None,
      referenceNumber = None,
      isVariation = false,
      dateReceived = new LocalDate(1970, 1, 31),
      isRead = false
    )

    val controller = new SecureCommunicationsController {
      override protected def authConnector: AuthConnector = self.authConnector
      override protected[controllers] val dataCacheConnector = mock[DataCacheConnector]

      private def getSecureComms: List[SecureCommunication] = List(
        testSecureComms.copy(messageType = Some(APA1)),
        testSecureComms.copy(isVariation = true),
        testSecureComms.copy(messageType = Some(APR1)),
        testSecureComms.copy(messageType = Some(REJR)),
        testSecureComms,
        testSecureComms.copy(messageType = Some(REVR)),
        testSecureComms.copy(messageType = Some(EXPR)),
        testSecureComms.copy(messageType = Some(RPA1)),
        testSecureComms.copy(messageType = Some(RPV1)),
        testSecureComms.copy(messageType = Some(RPR1)),
        testSecureComms.copy(messageType = Some(RPM1)),
        testSecureComms.copy(messageType = Some(RREM)),
        testSecureComms.copy(messageType = Some(MTRJ)),
        testSecureComms.copy(messageType = Some(MTRV)),
        testSecureComms.copy(messageType = Some(NMRJ)),
        testSecureComms.copy(messageType = Some(NMRV)),
        testSecureComms.copy(messageType = Some(OTHR))
      )
    }
  }

  "SubmissionController" must {
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
      document.getElementsByClass("heading-small").html() must include(testBusinessName)
      document.getElementsByTag("table").html() must include("Subject")
      document.getElementsByTag("table").html() must include("Date")
      document.getElementsByTag("table").html() must include("message-unread")
    }
    "get messages in chronological order (newest first)" in new Fixture {

    }
  }

}
