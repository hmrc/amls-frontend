/*
 * Copyright 2017 HM Revenue & Customs
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

import cats.implicits._
import connectors.{DataCacheConnector, FeeConnector}
import models.ResponseType.SubscriptionResponseType
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching._
import models.renewal._
import models.status._
import models.{status => _, _}
import org.joda.time.{DateTime, DateTimeZone, LocalDate, LocalDateTime}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, LandingService, RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class StatusControllerSpec extends GenericTestHelper with MockitoSugar with OneAppPerSuite {

  val cacheMap = mock[CacheMap]

  override lazy val app = GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-withdrawal" -> true)
    .build()

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val controller = new StatusController {
      override private[controllers] val landingService: LandingService = mock[LandingService]
      override val authConnector = self.authConnector
      override private[controllers] val enrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
      override private[controllers] val statusService: StatusService = mock[StatusService]
      override private[controllers] val feeConnector: FeeConnector = mock[FeeConnector]
      override private[controllers] val renewalService: RenewalService = mock[RenewalService]
    }
  }

  val amlsRegistrationNumber = "XAML00000567890"
  val feeResponse = FeeResponse(
    SubscriptionResponseType,
    amlsRegistrationNumber,
    150.00,
    Some(100.0),
    300.0,
    550.0,
    Some("XA353523452345"),
    None,
    new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC)
  )
  val reviewDetails = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
    Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

  "StatusController" should {
    "respond with OK and show business name on the status page" in new Fixture {


      when(controller.landingService.cacheMap(any(), any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
        .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

      when(controller.statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful((NotCompleted, None)))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("panel-indent").first().child(1).html() must be(reviewDetails.businessName)

    }

    "show correct content" when {

      "application status is NotCompleted" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((NotCompleted, None)))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.incomplete.heading"))
      }

      "application status is SubmissionReady" in new Fixture {

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(
            Some(BusinessMatching(Some(reviewDetails), None)))

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady, None))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissionready.heading"))
      }

      "application status is SubmissionReadyForReview" when {
        "there is no ReadStatusResponse" in new Fixture {

          when(controller.landingService.cacheMap(any(), any(), any()))
            .thenReturn(Future.successful(Some(cacheMap)))

          when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
            .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

          when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
            .thenReturn(Future.successful(None))

          when(controller.statusService.getDetailedStatus(any(), any(), any()))
            .thenReturn(Future.successful((SubmissionReadyForReview, None)))

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(Messages("status.submissionreadyforreview.heading"))
        }

        "there is ReadStatusResponse data" in new Fixture {

          when(controller.landingService.cacheMap(any(), any(), any()))
            .thenReturn(Future.successful(Some(cacheMap)))

          when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
            .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

          when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
            .thenReturn(Future.successful(None))

          private val readStatusResponse = Some(ReadStatusResponse(
            LocalDateTime.now,
            "formBundleStatus",
            None, None, None,
            Some(LocalDate.now.plusDays(15)),
            true, None, None, None
          ))

          when(controller.statusService.getDetailedStatus(any(), any(), any()))
            .thenReturn(Future.successful((SubmissionReadyForReview, readStatusResponse)))

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include(Messages("status.submissionreadyforreview.heading"))
        }
      }

      "application status is SubmissionDecisionApproved" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

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

        contentAsString(result) must include(Messages("status.submissiondecisionsupervised.heading"))
        contentAsString(result) mustNot include(Messages("status.submissiondecisionsupervised.renewal.btn"))
      }

      "application status is SubmissionDecisionRejected" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionRejected, None)))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecision.not.supervised.heading"))
      }

      "application status is SubmissionDecisionRevoked" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionRevoked, None)))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecision.not.supervised.heading"))

      }

      "application status is SubmissionDecisionExpired" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionExpired, None)))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecision.not.supervised.heading"))
      }

      "application status is SubmissionWithdrawn" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionWithdrawn, None)))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecision.not.supervised.heading"))
      }

      "application status is DeRegistered" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((DeRegistered, None)))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecision.not.supervised.heading"))
      }

      "application status is RenewalSubmitted" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((RenewalSubmitted(Some(LocalDate.now)), None)))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.renewalsubmitted.description"))
      }

      "application status is ReadyForRenewal, and the renewal has not been started" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

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

        when(controller.renewalService.getRenewal(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecisionsupervised.renewal.btn"))

      }

      "application status is ReadyForRenewal, and the renewal has been started but is incomplete" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.renewalService.isRenewalComplete(any())(any(), any(), any()))
          .thenReturn(Future.successful(false))

        val renewalDate = LocalDate.now().plusDays(15)

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None, Some(renewalDate), false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        when(controller.renewalService.getRenewal(any(), any(), any()))
          .thenReturn(Future.successful(Some(Renewal())))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.renewalincomplete.description"))

      }

      "application status is ReadyForRenewal, and the renewal is complete but not submitted" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](any())(any()))
          .thenReturn(Some(BusinessMatching(
            activities = Some(BusinessActivities(Set(
              MoneyServiceBusiness,
              HighValueDealing
            ))),
            msbServices = Some(MsbServices(Set(CurrencyExchange)))
          )))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some("amlsRegNo")))

        val dataCache = mock[DataCacheConnector]

        when(dataCache.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.renewalService.isRenewalComplete(any())(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(authConnector.currentAuthority(any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        val renewalDate = LocalDate.now().plusDays(15)

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None, Some(renewalDate), false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

        when(controller.feeConnector.feeResponse(any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(feeResponse))

        private val completeRenewal = Renewal(
          Some(InvolvedInOtherYes("test")),
          Some(BusinessTurnover.First),
          Some(AMLSTurnover.First),
          Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          Some(PercentageOfCashPaymentOver15000.First),
          Some(ReceiveCashPayments(Some(PaymentMethods(true, true, Some("other"))))),
          Some(TotalThroughput("01")),
          Some(WhichCurrencies(Seq("EUR"), None, None, None, None)),
          Some(TransactionsInLast12Months("1500")),
          Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
          Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
          Some(CETransactionsInLast12Months("123")),
          hasChanged = true
        )

        when(controller.renewalService.getRenewal(any(), any(), any()))
          .thenReturn(Future.successful(Some(completeRenewal)))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.renewalnotsubmitted.description"))

      }
    }

    "show the withdrawal link" when {
      "the status is 'ready for review'" in new Fixture {
        val reviewDetails = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

        val statusResponse = mock[ReadStatusResponse]
        when(statusResponse.processingDate).thenReturn(LocalDateTime.now)

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(
            Some(BusinessMatching(Some(reviewDetails), None)))

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview, statusResponse.some))

        val result = controller.get()(request)
        val doc = Jsoup.parse(contentAsString(result))

        doc.select(s"a[href=${controllers.routes.WithdrawApplicationController.get().url}]").text mustBe Messages("status.withdraw.link-text")
      }
    }

    "show the deregister link" when {
      "the status is 'approved'" in new Fixture {
        val reviewDetails = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

        val statusResponse = mock[ReadStatusResponse]
        when(statusResponse.currentRegYearEndDate).thenReturn(LocalDate.now.some)

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(
            Some(BusinessMatching(Some(reviewDetails), None)))

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved, statusResponse.some))

        val result = controller.get()(request)
        val doc = Jsoup.parse(contentAsString(result))

        doc.select(s"a[href=${controllers.routes.DeRegisterApplicationController.get().url}]").text mustBe Messages("status.deregister.link-text")
      }
    }
  }
}

class StatusControllerWithoutWithdrawalSpec extends GenericTestHelper with OneAppPerSuite {

  override lazy val app = GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-withdrawal" -> false)
    .build()

  trait Fixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val cacheMap = mock[CacheMap]

    val controller = new StatusController {
      override private[controllers] val landingService: LandingService = mock[LandingService]
      override val authConnector = self.authConnector
      override private[controllers] val enrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
      override private[controllers] val statusService: StatusService = mock[StatusService]
      override private[controllers] val feeConnector: FeeConnector = mock[FeeConnector]
      override private[controllers] val renewalService: RenewalService = mock[RenewalService]
    }

    val reviewDetails = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
      Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

    val statusResponse = mock[ReadStatusResponse]
    when(statusResponse.processingDate).thenReturn(LocalDateTime.now)

    when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
      .thenReturn(
        Some(BusinessMatching(Some(reviewDetails), None)))

    when(controller.landingService.cacheMap(any(), any(), any()))
      .thenReturn(Future.successful(Some(cacheMap)))

    when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
      .thenReturn(Future.successful(None))

    when(controller.statusService.getDetailedStatus(any(), any(), any()))
      .thenReturn(Future.successful(SubmissionReadyForReview, statusResponse.some))
  }

  "The status controller" must {
    "not show the withdrawal link" in new Fixture {
      val result = controller.get()(request)
      val doc = Jsoup.parse(contentAsString(result))

      Option(doc.select(s"a[href=${controllers.routes.WithdrawApplicationController.get().url}]").first()) must not be defined
    }
  }
}

class StatusControllerWithoutDeRegisterSpec extends GenericTestHelper with OneAppPerSuite {

  override lazy val app = GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.allow-deregister" -> false)
    .build()

  trait Fixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val cacheMap = mock[CacheMap]

    val controller = new StatusController {
      override private[controllers] val landingService: LandingService = mock[LandingService]
      override val authConnector = self.authConnector
      override private[controllers] val enrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
      override private[controllers] val statusService: StatusService = mock[StatusService]
      override private[controllers] val feeConnector: FeeConnector = mock[FeeConnector]
      override private[controllers] val renewalService: RenewalService = mock[RenewalService]
    }

    val reviewDetails = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
      Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

    val statusResponse = mock[ReadStatusResponse]
    when(statusResponse.currentRegYearEndDate).thenReturn(LocalDate.now.some)

    when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
      .thenReturn(
        Some(BusinessMatching(Some(reviewDetails), None)))

    when(controller.landingService.cacheMap(any(), any(), any()))
      .thenReturn(Future.successful(Some(cacheMap)))

    when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
      .thenReturn(Future.successful(None))

    when(controller.statusService.getDetailedStatus(any(), any(), any()))
      .thenReturn(Future.successful(SubmissionDecisionApproved, statusResponse.some))
  }

  "The status controller" must {
    "not show the deregister link" in new Fixture {
      val result = controller.get()(request)
      val doc = Jsoup.parse(contentAsString(result))

      Option(doc.select(s"a[href=${controllers.routes.WithdrawApplicationController.get().url}]").first()) must not be defined
    }
  }
}
