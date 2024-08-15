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

import cats.implicits._
import connectors._
import controllers.actions.{SuccessfulAuthAction, SuccessfulAuthActionNoAmlsRefNo}
import generators.PaymentGenerator
import models.ResponseType.SubscriptionResponseType
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.CurrencyExchange
import models.businessmatching._
import models.notifications.{IDType, NotificationRow}
import models.registrationdetails.RegistrationDetails
import models.renewal._
import models.responsiblepeople._
import models.status._
import models.{status => _, _}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.http.Status.OK
import play.api.i18n.Messages
import play.api.test.Helpers._
import play.api.test.Injecting
import services._
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks, FutureAssertions}
import views.html.status.YourRegistrationView
import views.html.status.components._

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class StatusControllerSpec extends AmlsSpec with PaymentGenerator with Injecting {

  val cacheMap = mock[Cache]

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)
    lazy val view = inject[YourRegistrationView]
    val controller = new StatusController(
      mock[LandingService],
      mock[StatusService],
      mock[AuthEnrolmentsService],
      mock[FeeConnector],
      mock[RenewalService],
      mock[ProgressService],
      mock[AmlsConnector],
      mockCacheConnector,
      SuccessfulAuthAction,
      commonDependencies,
      mock[FeeResponseService],
      mockMcc,
      mock[AmlsNotificationConnector],
      inject[FeeInformation],
      inject[RegistrationStatus],
      inject[ApplicationIncomplete],
      inject[ApplicationDeregistered],
      inject[ApplicationRenewalSubmissionReady],
      inject[ApplicationRenewalDue],
      inject[ApplicationSubmissionReady],
      inject[ApplicationPending],
      inject[ApplicationRejected],
      inject[ApplicationRevoked],
      inject[ApplicationExpired],
      inject[ApplicationWithdrawn],
      inject[ApplicationRenewalSubmitted],
      inject[ApplicationRenewalIncomplete],
      inject[WithdrawOrDeregisterInformation],
      inject[TradeInformationNoActivities],
      inject[TradeInformationOneActivity],
      inject[TradeInformation],
      inject[TradeInformationFindOut],
      view = view)

    val controllerNoAmlsNumber = new StatusController(
      mock[LandingService],
      mock[StatusService],
      mock[AuthEnrolmentsService],
      mock[FeeConnector],
      mock[RenewalService],
      mock[ProgressService],
      mock[AmlsConnector],
      mockCacheConnector,
      SuccessfulAuthActionNoAmlsRefNo,
      commonDependencies,
      mock[FeeResponseService],
      mockMcc,
      mock[AmlsNotificationConnector],
      inject[FeeInformation],
      inject[RegistrationStatus],
      inject[ApplicationIncomplete],
      inject[ApplicationDeregistered],
      inject[ApplicationRenewalSubmissionReady],
      inject[ApplicationRenewalDue],
      inject[ApplicationSubmissionReady],
      inject[ApplicationPending],
      inject[ApplicationRejected],
      inject[ApplicationRevoked],
      inject[ApplicationExpired],
      inject[ApplicationWithdrawn],
      inject[ApplicationRenewalSubmitted],
      inject[ApplicationRenewalIncomplete],
      inject[WithdrawOrDeregisterInformation],
      inject[TradeInformationNoActivities],
      inject[TradeInformationOneActivity],
      inject[TradeInformation],
      inject[TradeInformationFindOut],
      view = view)

    val positions = Positions(Set(BeneficialOwner, Partner, NominatedOfficer), Some(PositionStartDate(LocalDate.now())))
    val rp1 = ResponsiblePerson(
      personName = Some(PersonName("first1", Some("middle"), "last1")),
      legalName = None,
      legalNameChangeDate = None,
      knownBy = None,
      personResidenceType = None,
      ukPassport = None,
      nonUKPassport = None,
      dateOfBirth = None,
      contactDetails = None,
      addressHistory = None,
      positions = Some(positions)
    )
    val rp2 = ResponsiblePerson(
      personName = Some(PersonName("first2", None, "last2")),
      legalName = None,
      legalNameChangeDate = None,
      knownBy = None,
      personResidenceType = None,
      ukPassport = None,
      nonUKPassport = None,
      dateOfBirth = None,
      contactDetails = None,
      addressHistory = None,
      positions = Some(positions)
    )
    val responsiblePeople = Seq(rp1, rp2)
    val amlsRegistrationNumber = "amlsRefNumber"

    when(controller.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

    when(controllerNoAmlsNumber.enrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
      .thenReturn(Future.successful(None))

    when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful((NotCompleted, None)))

    mockCacheFetch[BusinessMatching](Some(BusinessMatching(Some(reviewDetails), None)), Some(BusinessMatching.key))
    mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))

    when(controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber), any[(String, String)]())(any(), any()))
      .thenReturn(Future.successful(Some(feeResponse)))

    when(controller.notificationConnector.fetchAllByAmlsRegNo(eqTo(amlsRegistrationNumber), any())(any(), any()))
      .thenReturn(Future.successful(Seq()))
  }

  val feeResponse = FeeResponse(
    SubscriptionResponseType,
    amlsRegistrationNumber,
    150.00,
    Some(100.0),
    None,
    300.0,
    550.0,
    Some("XA353523452345"),
    None,
    LocalDateTime.of(2017, 12, 1, 1, 3)
  )

  val reviewDetails = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
    Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

  val noMsbNoTcsp = Some(BusinessActivities(Set(TelephonePaymentService, BillPaymentServices, AccountancyServices, EstateAgentBusinessService)))
  val tcspAndOther = Some(BusinessActivities(Set(TelephonePaymentService, BillPaymentServices, AccountancyServices, EstateAgentBusinessService, TrustAndCompanyServices)))
  val msbAndOther = Some(BusinessActivities(Set(TelephonePaymentService, BillPaymentServices, AccountancyServices, EstateAgentBusinessService, MoneyServiceBusiness)))
  val msbAndTcsp = Some(BusinessActivities(Set(TelephonePaymentService, BillPaymentServices, AccountancyServices, EstateAgentBusinessService, MoneyServiceBusiness, TrustAndCompanyServices)))
  val onlyMsb = Some(BusinessActivities(Set(MoneyServiceBusiness)))
  val onlyTcsp = Some(BusinessActivities(Set(TrustAndCompanyServices)))
  val msbAndTcspOnly = Some(BusinessActivities(Set(TrustAndCompanyServices, MoneyServiceBusiness)))

  "StatusController" should {
    "respond with SEE_OTHER and redirect to the landing page" when {
      "status is rejected and the new submission button is selected" in new Fixture {

        when(controller.statusService.getStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionRejected))

        when(controller.enrolmentsService.deEnrol(any(), any())(any(), any()))
          .thenReturn(Future.successful(true))

        when(controller.dataCache.remove(any[String])).thenReturn(Future.successful(true))
          .thenReturn(Future.successful(true))

        val result = controller.newSubmission()(request)
        status(result) must be(SEE_OTHER)
        verify(controller.enrolmentsService).deEnrol(eqTo(amlsRegistrationNumber), any())(any(), any())
        redirectLocation(result) must be(Some(controllers.routes.LandingController.start(true).url))
      }

      "status is deregistered and the new submission button is selected" in new Fixture {

        when(controller.statusService.getStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(DeRegistered))

        when(controller.enrolmentsService.deEnrol(any(), any())(any(), any()))
          .thenReturn(Future.successful(true))

        when(controller.dataCache.remove(any[String])).thenReturn(Future.successful(true))
          .thenReturn(Future.successful(true))

        val result = controller.newSubmission()(request)
        status(result) must be(SEE_OTHER)
        verify(controller.enrolmentsService).deEnrol(eqTo(amlsRegistrationNumber), any())(any(), any())
        redirectLocation(result) must be(Some(controllers.routes.LandingController.start(true).url))
      }
    }

    "respond with OK and show business name on the status page" in new Fixture {
      when {
        controller.amlsConnector.registrationDetails(any(), any())(any(), any())
      } thenReturn Future.successful(RegistrationDetails("Test Company", isIndividual = false))

      when(controller.landingService.cacheMap(any[String]))
        .thenReturn(Future.successful(Some(cacheMap)))

      val statusResponse = mock[ReadStatusResponse]
      when(statusResponse.safeId) thenReturn Some("X12345678")

      when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful((NotCompleted, Some(statusResponse))))

      when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("business-name").html() must include("Test Company")
    }

    "show correct content" when {

      "application status is NotCompleted" in new Fixture {

        when(controllerNoAmlsNumber.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(controllerNoAmlsNumber.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((NotCompleted, None)))

        when(controllerNoAmlsNumber.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val result = controllerNoAmlsNumber.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.incomplete.heading"))
      }

      "application status is SubmissionReadyForReview" when {
        "there is a fee response available" in new Fixture {

          when(controller.landingService.cacheMap(any[String]))
            .thenReturn(Future.successful(Some(cacheMap)))

          when(controller.dataCache.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(Some(BusinessMatching(Some(reviewDetails), Some(BusinessActivities(Set(TelephonePaymentService)))))))

          when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful((SubmissionReadyForReview, None)))

          when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

          val result = controller.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include("If you do not pay your fees within 28 days of submitting your application it will be rejected.")
        }

        "there is no ReadStatusResponse" in new Fixture {
          when(controllerNoAmlsNumber.landingService.cacheMap(any[String]))
            .thenReturn(Future.successful(Some(cacheMap)))

          when(controllerNoAmlsNumber.dataCache.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(Some(BusinessMatching(Some(reviewDetails), Some(BusinessActivities(Set(TelephonePaymentService)))))))

          when(controllerNoAmlsNumber.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful((SubmissionReadyForReview, None)))

          when(controllerNoAmlsNumber.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

          val result = controllerNoAmlsNumber.get()(request)
          status(result) must be(OK)

          contentAsString(result) must include("If you do not pay your fees within 28 days of submitting your application it will be rejected.")
        }
      }

      "application status is SubmissionDecisionApproved" in new Fixture {

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.dataCache.fetch[BusinessMatching](any(), any())(any()))
          .thenReturn(Future.successful(Some(BusinessMatching(Some(reviewDetails), Some(BusinessActivities(Set(TelephonePaymentService)))))))

        when(cacheMap.getEntry[SubscriptionResponse](contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, None, None, 0, None, 0)))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
          Some(LocalDate.now.plusDays(30)), false)

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionApproved, Some(readStatusResponse))))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecisionsupervised.heading"))
        contentAsString(result) mustNot include(Messages("status.submissiondecisionsupervised.renewal.btn"))
      }

      "application status is SubmissionDecisionRejected" in new Fixture {

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, None, None, 0, None, 0)))))

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionRejected, None)))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecision.not.supervised.heading"))

        val html = Jsoup.parse(contentAsString(result))
        Option(html.getElementById("new.application.button")) mustBe defined
      }

      "application status is SubmissionDecisionRevoked and the submit button is allowed" in new Fixture {

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, None, None, 0, None, 0)))))

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionRevoked, None)))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecision.not.supervised.heading"))

        val html = Jsoup.parse(contentAsString(result))
        Option(html.getElementById("new.application.button")) mustBe defined

      }

      "application status is SubmissionDecisionExpired" in new Fixture {

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, None, None, 0, None, 0)))))

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionExpired, None)))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecision.not.supervised.heading"))
      }

      "application status is SubmissionWithdrawn" in new Fixture {

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, None, None, 0, None, 0)))))

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionWithdrawn, None)))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(messages("status.submissiondecision.not.supervised.heading"))
      }

      "application status is DeRegistered" in new Fixture {

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, None, None, 0, None, 0)))))

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((DeRegistered, None)))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(messages("status.submissiondecision.not.supervised.heading"))
      }

      "application status is RenewalSubmitted" in new Fixture {

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, None, None, 0, None, 0)))))

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((RenewalSubmitted(Some(LocalDate.now)), None)))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val result = controller.get()(request)
        status(result) must be(OK)

        val html = contentAsString(result)
        html must include(Messages("your.registration.status.renewal.submitted"))
      }

      "application status is ReadyForRenewal, and the renewal has not been started" in new Fixture {

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, None, None, 0, None, 0)))))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val renewalDate = LocalDate.now().plusDays(15)

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
          Some(renewalDate), false)

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

        when(controller.renewalService.getRenewal(any[String]())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecisionsupervised.renewal.btn"))

      }

      "application status is ReadyForRenewal, and the renewal has been started but is incomplete" in new Fixture {

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, None, None, 0, None, 0)))))

        when(controller.renewalService.isRenewalComplete(any(), any[String]())(any()))
          .thenReturn(Future.successful(false))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val renewalDate = LocalDate.now().plusDays(15)

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
          Some(renewalDate), false)

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

        when(controller.renewalService.getRenewal(any[String]())).thenReturn(Future.successful(Some(Renewal())))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.renewalincomplete.description"))

      }

      "application status is ReadyForRenewal, and the renewal is complete but not submitted" in new Fixture {

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](any())(any()))
          .thenReturn(Some(BusinessMatching(
            activities = Some(BusinessActivities(Set(
              MoneyServiceBusiness,
              HighValueDealing
            ))),
            msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange))),
            reviewDetails = Some(ReviewDetails("BusinessName", None, mock[Address], "safeId", None))
          )))

        when(cacheMap.getEntry[SubscriptionResponse](contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, None, None, 0, None, 0)))))

        val dataCache = mock[DataCacheConnector]

        when(dataCache.fetchAll(any[String]()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.renewalService.isRenewalComplete(any(), any[String]())(any()))
          .thenReturn(Future.successful(true))

        val renewalDate = LocalDate.now().plusDays(15)

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
          Some(renewalDate), false)

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        private val completeRenewal = Renewal(
          Some(InvolvedInOtherYes("test")),
          Some(BusinessTurnover.First),
          Some(AMLSTurnover.First),
          Some(AMPTurnover.First),
          Some(CustomersOutsideIsUK(true)),
          Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          Some(PercentageOfCashPaymentOver15000.First),
          Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other")))))),
          Some(TotalThroughput("01")),
          Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
          Some(TransactionsInLast12Months("1500")),
          Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
          Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
          Some(CETransactionsInLast12Months("123")),
          hasChanged = true
        )

        when(controller.renewalService.getRenewal(any[String]())).thenReturn(Future.successful(Some(completeRenewal)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val html = contentAsString(result)
        html must include(Messages("status.renewalnotsubmitted.description"))
      }

      "the status is 'approved' and there is no current mongo cache" in new Fixture {
        val reviewDetails = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

        val statusResponse = mock[ReadStatusResponse]
        when(statusResponse.currentRegYearEndDate).thenReturn(LocalDate.now.some)
        when(statusResponse.safeId).thenReturn(None)

        when(controller.dataCache.fetch[BusinessMatching](any(), any())(any()))
          .thenReturn(Future.successful(Some(BusinessMatching(Some(reviewDetails), Some(BusinessActivities(Set(TelephonePaymentService)))))))

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionApproved, statusResponse.some)))

        when(controller.landingService.refreshCache(any(), any(), any())(any(), any(), any())).thenReturn(Future.successful(cacheMap))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(false))

        val result = controller.get()(request)

        redirectLocation(result) must be(Some(controllers.routes.LandingController.get().url))
      }
    }

    "show the withdrawal link" when {
      "the status is 'ready for review'" in new Fixture {
        val reviewDetails = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

        val statusResponse = mock[ReadStatusResponse]
        when(statusResponse.processingDate).thenReturn(LocalDateTime.now)
        when(statusResponse.safeId).thenReturn(None)

        when(controller.dataCache.fetch[BusinessMatching](any(), any())(any()))
          .thenReturn(Future.successful(Some(BusinessMatching(Some(reviewDetails), Some(BusinessActivities(Set(TelephonePaymentService)))))))

        when(controllerNoAmlsNumber.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controllerNoAmlsNumber.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionReadyForReview, statusResponse.some)))

        when(controllerNoAmlsNumber.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val result = controllerNoAmlsNumber.get()(request)
        val doc = Jsoup.parse(contentAsString(result))

        doc.select(s"a[href=${controllers.withdrawal.routes.WithdrawApplicationController.get().url}]").text mustBe s"${messages("status.withdraw.link-text")}."
      }
    }

    "show the deregister link" when {
      "the status is 'approved'" in new Fixture {
        val reviewDetails = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

        val statusResponse = mock[ReadStatusResponse]
        when(statusResponse.currentRegYearEndDate).thenReturn(LocalDate.now.some)
        when(statusResponse.safeId).thenReturn(None)

        when(controller.dataCache.fetch[BusinessMatching](any(), any())(any()))
          .thenReturn(Future.successful(Some(BusinessMatching(Some(reviewDetails), Some(BusinessActivities(Set(TelephonePaymentService)))))))

        when(controller.landingService.cacheMap(any[String]))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionApproved, statusResponse.some)))

        when(controller.renewalService.isCachePresent(any())(any())).thenReturn(Future.successful(true))

        val result = controller.get()(request)
        val doc = Jsoup.parse(contentAsString(result))

        doc.select(s"a[href=${controllers.deregister.routes.DeRegisterApplicationController.get().url}]").text mustBe s"${messages("your.registration.deregister.link")}."
      }
    }

    "have hasMsb method which" must {
      "return true if Msb activities are present in business activities" in new Fixture {
        controller.hasMsb(msbAndOther) mustBe true
      }

      "return false if Msb activities are not present in business activities" in new Fixture {
        controller.hasMsb(noMsbNoTcsp) mustBe false
      }
    }

    "have hasTcsp method which" must {
      "return true if Tcsp activities are present in business activities" in new Fixture {
        controller.hasTcsp(tcspAndOther) mustBe true
      }

      "return false if Tcsp activities are not present in business activities" in new Fixture {
        controller.hasTcsp(noMsbNoTcsp) mustBe false
      }
    }

    "have hasOther method which" must {
      "return true if there are other activities than Msb present" in new Fixture {
        controller.hasOther(noMsbNoTcsp) mustBe true
      }

      "return false if there are no other activities than Msb or Tcsp" in new Fixture {
        controller.hasOther(onlyMsb) mustBe false
      }
    }

    "have canOrCannotTradeInformation method which" must {
      "return correct content if no msb or tcsp in BA" in new Fixture {
        val result = controller.canOrCannotTradeInformation(noMsbNoTcsp)

        result.body must include("You can trade and carry out business activities while your application is pending")
      }

      "return correct content if msb only in BA" in new Fixture {
        val result = controller.canOrCannotTradeInformation(onlyMsb)

        result.body must include("You cannot trade or carry out business activities while your application is pending")
      }

      "return correct content if tcsp only in BA" in new Fixture {
        val result = controller.canOrCannotTradeInformation(onlyTcsp)

        result.body must include("You cannot trade or carry out business activities while your application is pending")
      }

      "return correct content if tcsp and msb only in BA" in new Fixture {
        val result = controller.canOrCannotTradeInformation(msbAndTcspOnly)

        result.body must include("You cannot trade or carry out business activities while your application is pending")
      }

      "return correct content if msb and other in BA" in new Fixture {
        val result = controller.canOrCannotTradeInformation(msbAndOther)

        result.body must include("There are some services you cannot provide while your application is pending")
        result.body must include("https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register")
        result.body must include("Find out if you can trade")
      }

      "return correct content if tcsp and other in BA" in new Fixture {
        val result = controller.canOrCannotTradeInformation(tcspAndOther)

        result.body must include("There are some services you cannot provide while your application is pending")
        result.body must include("https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register")
        result.body must include("Find out if you can trade")
      }

      "return correct content if tcsp, msb and other in BA" in new Fixture {
        val result = controller.canOrCannotTradeInformation(msbAndTcsp)

        result.body must include("There are some services you cannot provide while your application is pending")
        result.body must include("https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register")
        result.body must include("Find out if you can trade")
      }

      "return default content if BA is empty" in new Fixture {
        val result = controller.canOrCannotTradeInformation(None)

        result.body must include("https://www.gov.uk/guidance/money-laundering-regulations-who-needs-to-register")
        result.body must include("Find out if you can trade")
      }
    }

    "have countUnreadNotifications method which" must {
      "return 0 if there is no amls ref number or safe id available" in new Fixture with FutureAssertions {

        val result = controller.countUnreadNotifications(None, None, ("", ""))

        result returns 0
      }

      "return 0 if there is amls ref number available but no notifications returned" in new Fixture with FutureAssertions {

        when(controller.notificationConnector.fetchAllByAmlsRegNo(eqTo(amlsRegistrationNumber), any())(any(), any()))
          .thenReturn(Future.successful(Seq()))

        val result = controller.countUnreadNotifications(Some(amlsRegistrationNumber), None, ("", ""))

        result returns 0
      }

      "return 1 if there is amls ref number available and 1 unread notification" in new Fixture with FutureAssertions {

        when(controller.notificationConnector.fetchAllByAmlsRegNo(eqTo(amlsRegistrationNumber), any())(any(), any()))
          .thenReturn(Future.successful(Seq(NotificationRow(None, None, None, false, LocalDateTime.now(), false, amlsRegistrationNumber, "", IDType("")))))

        val result = controller.countUnreadNotifications(Some(amlsRegistrationNumber), None, ("", ""))

        result returns 1
      }

      "return 1 if there is amls ref number available and 1 unread notification and 1 read notification" in new Fixture with FutureAssertions {

        when(controller.notificationConnector.fetchAllByAmlsRegNo(eqTo(amlsRegistrationNumber), any())(any(), any()))
          .thenReturn(Future.successful(Seq(NotificationRow(None, None, None, false, LocalDateTime.now(), false, amlsRegistrationNumber, "", IDType("")),
            NotificationRow(None, None, None, false, LocalDateTime.now(), true, amlsRegistrationNumber, "", IDType("")))))

        val result = controller.countUnreadNotifications(Some(amlsRegistrationNumber), None, ("", ""))

        result returns 1
      }

      "return 0 if there is amls ref number available and 1 read notification" in new Fixture with FutureAssertions {

        when(controller.notificationConnector.fetchAllByAmlsRegNo(eqTo(amlsRegistrationNumber), any())(any(), any()))
          .thenReturn(Future.successful(Seq(NotificationRow(None, None, None, false, LocalDateTime.now(), true, amlsRegistrationNumber, "", IDType("")))))

        val result = controller.countUnreadNotifications(Some(amlsRegistrationNumber), None, ("", ""))

        result returns 0
      }

      "return 0 if there is safe id available but no notifications returned" in new Fixture with FutureAssertions {

        when(controller.notificationConnector.fetchAllBySafeId(eqTo("internalId"), any())(any(), any()))
          .thenReturn(Future.successful(Seq()))

        val result = controller.countUnreadNotifications(None, Some("internalId"), ("", ""))

        result returns 0
      }

      "return 1 if there is safe id available and 1 unread notification" in new Fixture with FutureAssertions {

        when(controller.notificationConnector.fetchAllBySafeId(eqTo("internalId"), any())(any(), any()))
          .thenReturn(Future.successful(Seq(NotificationRow(None, None, None, false, LocalDateTime.now(), false, amlsRegistrationNumber, "", IDType("")))))

        val result = controller.countUnreadNotifications(None, Some("internalId"), ("", ""))

        result returns 1
      }

      "return 1 if there is safe id available and 1 unread notification and 1 read notification" in new Fixture with FutureAssertions {

        when(controller.notificationConnector.fetchAllBySafeId(eqTo("internalId"), any())(any(), any()))
          .thenReturn(Future.successful(Seq(NotificationRow(None, None, None, false, LocalDateTime.now(), false, amlsRegistrationNumber, "", IDType("")),
            NotificationRow(None, None, None, false, LocalDateTime.now(), true, amlsRegistrationNumber, "", IDType("")))))

        val result = controller.countUnreadNotifications(None, Some("internalId"), ("", ""))

        result returns 1
      }

      "return 0 if there is safe id available and 1 read notification" in new Fixture with FutureAssertions {

        when(controller.notificationConnector.fetchAllBySafeId(eqTo("internalId"), any())(any(), any()))
          .thenReturn(Future.successful(Seq(NotificationRow(None, None, None, false, LocalDateTime.now(), true, amlsRegistrationNumber, "", IDType("")))))

        val result = controller.countUnreadNotifications(None, Some("internalId"), ("", ""))

        result returns 0
      }
    }
  }
}
