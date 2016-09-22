package controllers

import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.status._
import models.{Country, ReadStatusResponse, SubscriptionResponse}
import org.joda.time.LocalDateTime
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, LandingService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class StatusControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new StatusController {
      override private[controllers] val landingService: LandingService = mock[LandingService]
      override val authConnector = self.authConnector
      override private[controllers] val enrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
      override private[controllers] val statusService: StatusService = mock[StatusService]
    }
  }

  "StatusController" should {

    "load the status page" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

      val cacheMap = mock[CacheMap]
      when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

      when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any())).thenReturn(
        Some(BusinessMatching(Some(reviewDtls), None)))

      when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(NotCompleted))
      when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))

      document.getElementsByClass("heading-secondary").first().html() must be(Messages("summary.status"))
      document.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

      document.getElementsByClass("list").first().child(0).html() must be(Messages("status.incomplete"))
      document.getElementsByClass("list").first().child(1).html() must be(Messages("status.notsubmitted"))
      document.getElementsByClass("list").first().child(2).html() must be(Messages("status.underreview"))
      document.getElementsByClass("list").first().child(3).html() must be(Messages("status.decisionmade"))

    }

    "show business name " in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

      val cacheMap = mock[CacheMap]
      when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

      when(cacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(
        Some(BusinessMatching(Some(reviewDtls), None)))
      when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(None))
      when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(NotCompleted))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("panel-indent").first().child(1).html() must be(reviewDtls.businessName)

    }

    "show correct status classes  " when {

      "submission incomplete" in new Fixture {



        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

        val cacheMap = mock[CacheMap]
        when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))
        when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(None))
        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any())).thenReturn(
          Some(BusinessMatching(Some(reviewDtls), None)))

        when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(NotCompleted))


        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("status-list").first().child(0).hasClass("current") must be(true)

        for (index <- 1 to 3) {
          document.getElementsByClass("status-list").first().child(index).hasClass("current") must be(false)
        }
        document.title() must be(Messages("status.incomplete.heading")+" - Your registration - Anti-money laundering registration - GOV.UK")

        document.getElementsByClass("status-detail").first().child(0).html() must be(Messages("status.incomplete.description"))


      }

      "submission completed" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

        val cacheMap = mock[CacheMap]
        when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any())).thenReturn(
          Some(BusinessMatching(Some(reviewDtls), None)))
        when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(None))
        when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(SubmissionReady))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("declaration").first().child(0).html() must be(Messages("status.hassomethingchanged"))
        document.getElementsByClass("status-list").first().child(0).hasClass("status-list--complete") must be(true)
        document.getElementsByClass("status-list").first().child(1).hasClass("current") must be(true)

        for (index <- 2 to 3) {
          document.getElementsByClass("status-list").first().child(index).hasClass("status-list--upcoming") must be(true)
        }
        document.title() must be(Messages("status.submissionready.heading")+" - Your registration - Anti-money laundering registration - GOV.UK")

        document.getElementsByClass("status-detail").first().child(0).html() must be(Messages("status.submissionready.description"))

      }


      "under review" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

        val cacheMap = mock[CacheMap]
        when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any())).thenReturn(
          Some(BusinessMatching(Some(reviewDtls), None)))


        when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any())) thenReturn Future.successful(Some(authority.copy(enrolments = Some("bar"))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, false)

        when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(SubmissionReadyForReview))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        for (index <- 0 to 1) {
          document.getElementsByClass("status-list").first().child(index).hasClass("status-list--complete") must be(true)
        }

        document.getElementsByClass("status-list").first().child(2).hasClass("current") must be(true)

        document.getElementsByClass("status-list").first().child(3).hasClass("status-list--upcoming") must be(true)
        document.title() must be(Messages("status.submissionreadyforreview.heading")+" - Your registration - Anti-money laundering registration - GOV.UK")

        document.getElementsByClass("status-detail").first().child(0).html() must be(Messages("status.submissionreadyforreview.description"))
        document.getElementsByClass("status-detail").first().child(1).html() must be(Messages("status.submissionreadyforreview.description2"))

      }

      "decision made (approved)" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

        val cacheMap = mock[CacheMap]
        when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any())).thenReturn(
          Some(BusinessMatching(Some(reviewDtls), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any())).thenReturn(
          Some(SubscriptionResponse("","",0,None,0,0,"")))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any())) thenReturn Future.successful(Some(authority.copy(enrolments = Some("bar"))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None, false)

        when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        for (index <- 0 to 2) {
          document.getElementsByClass("status-list").first().child(index).hasClass("status-list--complete") must be(true)
        }

        document.getElementsByClass("status-list").first().child(3).hasClass("current") must be(true)

        document.title() must be(Messages("status.submissiondecisionapproved.heading")+" - Your registration - Anti-money laundering registration - GOV.UK")

        document.getElementsByClass("status-detail").first().child(0).html() must be(Messages("status.submissiondecisionapproved.description"))
        document.getElementsByClass("status-detail").first().child(1).html() must be(Messages("status.submissiondecisionapproved.description2"))

      }

      "decision made (rejected)" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

        val cacheMap = mock[CacheMap]
        when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any())).thenReturn(
          Some(BusinessMatching(Some(reviewDtls), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any())).thenReturn(
          Some(SubscriptionResponse("","",0,None,0,0,"")))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any())) thenReturn Future.successful(Some(authority.copy(enrolments = Some("bar"))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Rejected", None, None, None, false)

        when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(SubmissionDecisionRejected))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        for (index <- 0 to 2) {
          document.getElementsByClass("status-list").first().child(index).hasClass("status-list--complete") must be(true)
        }

        document.getElementsByClass("status-list").first().child(3).hasClass("current") must be(true)
        document.title() must be(Messages("status.submissiondecisionrejected.heading")+" - Your registration - Anti-money laundering registration - GOV.UK")

        document.getElementsByClass("status-detail").first().child(0).html() must be(Messages("status.submissiondecisionrejected.description"))

      }
    }
  }

  "show the correct content to edit submission" when {
    "application has not yet been submitted" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

      val cacheMap = mock[CacheMap]
      when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

      when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any())).thenReturn(
        Some(BusinessMatching(Some(reviewDtls), None)))

      when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))

      document.getElementsByClass("statusblock").first().html() must include(Messages("status.hassomethingchanged"))
      document.getElementsByClass("statusblock").first().html() must include(Messages("status.submissionready.changelink1"))

    }

    "application is in review" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "XE0001234567890")

      val cacheMap = mock[CacheMap]
      when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))

      when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any())).thenReturn(
        Some(BusinessMatching(Some(reviewDtls), None)))

      when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(Some("XAML00000567890")))
      val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, false)
      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))

      document.getElementsByClass("statusblock").html() must include(Messages("status.hassomethingchanged"))
      document.getElementsByClass("statusblock").html() must include(Messages("status.amendment.edit"))

    }
  }

}
