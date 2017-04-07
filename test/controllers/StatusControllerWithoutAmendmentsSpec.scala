package controllers

import connectors.{AmlsNotificationConnector, FeeConnector}
import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.status._
import models.{AmendVariationResponse, Country, FeeResponse, ReadStatusResponse, SubscriptionResponse}
import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, LandingService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.NotFoundException
import utils.AuthorisedFixture

import scala.concurrent.Future
class StatusControllerWithoutAmendmentsSpec extends GenericTestHelper with MockitoSugar {

  val cacheMap = mock[CacheMap]

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new StatusController {
      override private[controllers] val landingService: LandingService = mock[LandingService]
      override val authConnector = self.authConnector
      override private[controllers] val enrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
      override private[controllers] val statusService: StatusService = mock[StatusService]
      override private[controllers] val feeConnector: FeeConnector = mock[FeeConnector]
    }
  }

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.amendments" -> false) )

  "StatusController" must {
    "hide amendment/variation link when amendments toggle is off" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

      when(controller.landingService.cacheMap(any(), any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

      when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(Some("XAML00000567890")))

      when(controller.statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReadyForReview, None))

      when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mock[FeeResponse]))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("statusblock").html() must include(Messages("status.amendment.edit"))
    }
  }
}
