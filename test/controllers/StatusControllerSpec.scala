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
    val amlsRegistrationNumber = "XAML00000567890"
    val feeResponse = FeeResponse(SubscriptionResponseType, amlsRegistrationNumber
      , 150.00, Some(100.0), 300.0, 550.0, Some("XA353523452345"), None,
      new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC))

    val pageTitleSuffix = " - Your registration - " +Messages("title.amls") + " - " + Messages("title.gov")


    "load the status page" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

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

      val document = Jsoup.parse(contentAsString(result))

      document.getElementsByClass("heading-secondary").first().html() must include(Messages("summary.status"))
      document.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

      document.getElementsByClass("list").first().child(0).html() must include(Messages("status.incomplete"))
      document.getElementsByClass("list").first().child(1).html() must include(Messages("status.submitted"))
      document.getElementsByClass("list").first().child(2).html() must include(Messages("status.underreview"))

    }

    "show business name" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

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
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

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

        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("status-list").first().child(0).hasClass("status-list--start") must be(true)

        for (index <- 1 to 2) {
          document.getElementsByClass("status-list").first().child(index).hasClass("status-list--start") must be(false)
        }
        document.title() must be(Messages("status.incomplete.heading") + pageTitleSuffix)

        document.getElementsMatchingOwnText(Messages("status.incomplete.description")).text must be(Messages("status.incomplete.description"))

      }

      "submission completed" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

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
        document.getElementsByClass("declaration").first().child(0).html() must be(Messages("status.hassomethingchanged"))
        document.getElementsByClass("status-list").first().child(0).hasClass("status-list--complete") must be(true)
        document.getElementsByClass("status-list").first().child(1).hasClass("status-list--pending") must be(true)
        document.getElementsByClass("status-list").first().child(2).hasClass("status-list--upcoming") must be(true)

        document.title() must be(Messages("status.submissionready.heading") + pageTitleSuffix)

        document.getElementsMatchingOwnText(Messages("status.submissionready.description")).text() must be(Messages("status.submissionready.description"))
        document.getElementsByTag("details").html() must be("")
      }


      "under review" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

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

        val document = Jsoup.parse(contentAsString(result))

        for (index <- 0 to 1) {
          document.getElementsByClass("status-list").first().child(index).hasClass("status-list--complete") must be(true)
        }

        document.getElementsByClass("status-list").first().child(2).hasClass("status-list--end") must be(true)

        document.title() must be(Messages("status.submissionreadyforreview.heading") + pageTitleSuffix)

        document.getElementsMatchingOwnText(Messages("status.submissionreadyforreview.description")).text must be(Messages("status.submissionreadyforreview.description"))
        document.getElementsMatchingOwnText(Messages("status.submissionreadyforreview.description2")).text must be(Messages("status.submissionreadyforreview.description2"))
        document.getElementsByTag("details").first().child(0).html() must be(Messages("status.fee.link"))

        val date = DateHelper.formatDate(LocalDate.now())
        document.getElementsMatchingOwnText(Messages("status.submittedForReview.submitteddate.text")).text must be
        Messages("status.submittedForReview.submitteddate.text", date)
      }


      "under review and FeeResponse is failed" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

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

        val document = Jsoup.parse(contentAsString(result))

        for (index <- 0 to 1) {
          document.getElementsByClass("status-list").first().child(index).hasClass("status-list--complete") must be(true)
        }

        document.getElementsByClass("status-list").first().child(2).hasClass("status-list--end") must be(true)

        document.title() must be(Messages("status.submissionreadyforreview.heading") + pageTitleSuffix)

        document.getElementsMatchingOwnText(Messages("status.submissionreadyforreview.description")).text must be(Messages("status.submissionreadyforreview.description"))
        document.getElementsMatchingOwnText(Messages("status.submissionreadyforreview.description2")).text must be(Messages("status.submissionreadyforreview.description2"))
        document.getElementsByTag("details").html() must be("")
      }

      "decision made (approved)" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

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

        val document = Jsoup.parse(contentAsString(result))

        document.title() must be(Messages("status.submissiondecisionsupervised.heading") + pageTitleSuffix)

        document.getElementsMatchingOwnText(Messages("status.submissiondecisionsupervised.success.description")).text must be(Messages("status.submissiondecisionsupervised.success.description"))
        val date = DateHelper.formatDate(LocalDate.now().plusDays(30))
        document.getElementsMatchingOwnText(Messages("status.submissiondecisionsupervised.enddate.text")).text must be
        Messages("status.submissiondecisionsupervised.enddate.text", date)
      }


      "decision made (rejected)" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

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

        val document = Jsoup.parse(contentAsString(result))

        document.title() must be(Messages("status.submissiondecisionrejected.heading") + pageTitleSuffix)

        document.getElementsMatchingOwnText(Messages("status.submissiondecisionrejected.description")).text must be(Messages("status.submissiondecisionrejected.description"))
        document.getElementsMatchingOwnText(Messages("status.submissiondecisionrejected.description2")).text must be(Messages("status.submissiondecisionrejected.description2"))
      }
    }

    "ready for renewal" in new Fixture {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

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

      contentAsString(result) must include(Messages("status.readyforrenewal.warning",DateHelper.formatDate(renewalDate)))

      val document = Jsoup.parse(contentAsString(result))

      document.title() must be(Messages("status.submissiondecisionsupervised.heading") + pageTitleSuffix)

    }

    "show the correct content to edit submission" when {

      val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
        Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

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

        val document = Jsoup.parse(contentAsString(result))

        document.getElementsByClass("statusblock").first().html() must include(Messages("status.hassomethingchanged"))
        document.getElementsByClass("statusblock").first().html() must include(Messages("status.submissionready.changelink1"))

        document.html() must not include Messages("survey.satisfaction.beforeyougo")

        document.getElementsByClass("govuk-box-highlight messaging").size() mustBe 0

      }

      "application is in review" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("XAML00000567890")))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview, None))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.getElementsByClass("statusblock").html() must include(Messages("status.hassomethingchanged"))
        document.getElementsByClass("statusblock").html() must include(Messages("status.amendment.edit"))

        document.html() must include(Messages("survey.satisfaction.beforeyougo"))
        document.html() must include(Messages("survey.satisfaction.please"))
        document.html() must include(Messages("survey.satisfaction.answer"))
        document.html() must include(Messages("survey.satisfaction.helpus"))

        document.getElementsByClass("messaging").size() mustBe 1

      }

      "application has been approved" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("XBML00000567890")))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved, None))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.getElementsByClass("statusblock").html() must include(Messages("status.hassomethingchanged"))
        document.getElementsByClass("statusblock").html() must include(Messages("status.amendment.edit"))

        document.html() must include(Messages("survey.satisfaction.beforeyougo"))
        document.html() must include(Messages("survey.satisfaction.please"))
        document.html() must include(Messages("survey.satisfaction.answer"))
        document.html() must include(Messages("survey.satisfaction.helpus"))

        document.getElementsByClass("messaging").size() mustBe 1

      }
    }
  }
}



