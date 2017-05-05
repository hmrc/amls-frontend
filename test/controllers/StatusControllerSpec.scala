package controllers

import connectors.{AmlsNotificationConnector, FeeConnector}
import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.status._
import models.{AmendVariationResponse, Country, FeeResponse, ReadStatusResponse, SubscriptionResponse}
import org.joda.time.{DateTime, DateTimeZone, LocalDate, LocalDateTime}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.{AuthorisedFixture, DateHelper, GenericTestHelper}
import play.api.i18n.Messages
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, LandingService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.NotFoundException

import scala.concurrent.Future

class StatusControllerSpec extends GenericTestHelper with MockitoSugar {

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

  "StatusController" should {
    val amlsRegistrationNumber = "XAML00000000000"
    val feeResponse = FeeResponse(SubscriptionResponseType, amlsRegistrationNumber
      , 150.00, Some(100.0), 300.0, 550.0, Some("XA000000000000"), None,
      new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC))

    val pageTitleSuffix = " - Your registration - " +Messages("title.amls") + " - " + Messages("title.gov")


    "load the status page" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

      when(controller.landingService.cacheMap(any(), any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

      when(controller.statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful((NotCompleted, None)))

      when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "show business name" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

      when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

      when(cacheMap.getEntry[BusinessMatching](any())(any()))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

      when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful((NotCompleted, None)))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("panel-indent").first().child(1).html() must be(reviewDtls.businessName)

    }

    "show correct status classes  " when {

      "submission incomplete" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((NotCompleted, None)))
        val result = controller.get()(request)
        status(result) must be(OK)

      }

      "submission completed" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionReady, None)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
      }

      "under review" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any())) thenReturn Future.successful(Some(authority.copy(enrolments = Some("bar"))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, None, false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionReadyForReview, None)))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)

      }

      "under review and FeeResponse is failed" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, None, false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionReadyForReview, None)))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.failed(new NotFoundException("")))

        val result = controller.get()(request)
        status(result) must be(OK)

      }

      "decision made (approved)" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", 0, None, None, 0, None, 0, "")))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None, Some(LocalDate.now.plusDays(30)), false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionApproved, Some(readStatusResponse))))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)
      }


      "decision made (rejected)" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", 0, None, None, 0, None, 0, "")))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Rejected", None, None, None, None, false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionRejected, None)))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)

      }

      "decision made (Expired)" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", 0, None, None, 0, None, 0, "")))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Expired", None, None, None, None, false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionExpired, None)))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)
      }

      "decision made (Revoked)" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", 0, None, None, 0, None, 0, "")))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Revoked", None, None, None, None, false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionRevoked, None)))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)

      }
    }

    "ready for renewal" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

      when(controller.landingService.cacheMap(any(), any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
        .thenReturn(Some(BusinessMatching(Some(reviewDtls), None)))

      when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
        .thenReturn(Some(SubscriptionResponse("", "", 0, None, None, 0, None, 0, "")))

      when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(Some("amlsRegNo")))

      when(authConnector.currentAuthority(any()))
        .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

      val renewalDate = LocalDate.now().plusDays(15)

      val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None, Some(renewalDate), false)

      when(controller.statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

      when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(feeResponse))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "show the correct content to edit submission" when {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0000000000000")

      when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
        .thenReturn(
        Some(BusinessMatching(Some(reviewDtls), None)))

      "application has not yet been submitted" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any())).
          thenReturn(Future.successful(Some(cacheMap)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady, None))

        val result = controller.get()(request)
        status(result) must be(OK)
      }

      "application has been approved" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("XBML00000000000")))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved, None))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)
      }
    }
  }
}



