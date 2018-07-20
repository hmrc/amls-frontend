/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.{AmlsConnector, DataCacheConnector, FeeConnector}
import generators.PaymentGenerator
import models.ResponseType.SubscriptionResponseType
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching._
import models.registrationdetails.RegistrationDetails
import models.renewal._
import models.responsiblepeople.{PersonName, _}
import models.status._
import models.{status => _, _}
import org.joda.time.{DateTime, DateTimeZone, LocalDate, LocalDateTime}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status.OK
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class StatusControllerSpec extends AmlsSpec with MockitoSugar with OneAppPerSuite with PaymentGenerator {

  val cacheMap = mock[CacheMap]

  override lazy val app = GuiceApplicationBuilder().build()

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new StatusController (
       mock[LandingService],
       mock[StatusService],
       mock[AuthEnrolmentsService],
       mock[FeeConnector],
       mock[RenewalService],
       mock[ProgressService],
       mock[AmlsConnector],
       mockCacheConnector,
       self.authConnector,
       mock[FeeResponseService]
    )

    val positions = Positions(Set(BeneficialOwner, Partner, NominatedOfficer), Some(new LocalDate()))
    val rp1 = ResponsiblePerson(Some(PersonName("first1", Some("middle"), "last1")), None, None, None, None, None, None, None, None, None, Some(positions))
    val rp2 = ResponsiblePerson(Some(PersonName("first2", None, "last2")), None, None, None, None, None, None, None, None, None, Some(positions))
    val responsiblePeople = Seq(rp1, rp2)

    when(controller.statusService.getDetailedStatus(any(), any(), any()))
      .thenReturn(Future.successful((NotCompleted, None)))

    mockCacheFetch[BusinessMatching](Some(BusinessMatching(Some(reviewDetails), None)), Some(BusinessMatching.key))
    mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))

    when(controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(), any(), any()))
      .thenReturn(Future.successful(Some(feeResponse)))

    when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
      .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

  }

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
    "respond with SEE_OTHER and redirect to the landing page" when {
      "status is rejected and the new submission button is selected" in new Fixture {

        val httpResponse = mock[HttpResponse]

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionRejected))

        when(controller.enrolmentsService.deEnrol(any())(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(controller.dataCache.remove(any(), any()))
          .thenReturn(Future.successful(true))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

        val result = controller.newSubmission()(request)
        status(result) must be(SEE_OTHER)
        verify(controller.enrolmentsService).deEnrol(eqTo(amlsRegistrationNumber))(any(), any(), any())
        redirectLocation(result) must be(Some(controllers.routes.LandingController.start(true).url))
      }

      "status is deregistered and the new submission button is selected" in new Fixture {

        val httpResponse = mock[HttpResponse]

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(DeRegistered))

        when(controller.enrolmentsService.deEnrol(any())(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(controller.dataCache.remove(any(), any()))
          .thenReturn(Future.successful(true))

        when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
          .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

        val result = controller.newSubmission()(request)
        status(result) must be(SEE_OTHER)
        verify(controller.enrolmentsService).deEnrol(eqTo(amlsRegistrationNumber))(any(), any(), any())
        redirectLocation(result) must be(Some(controllers.routes.LandingController.start(true).url))
      }
    }

    "respond with OK and show business name on the status page" in new Fixture {
      when {
        controller.amlsConnector.registrationDetails(any())(any(), any(), any())
      } thenReturn Future.successful(RegistrationDetails("Test Company", isIndividual = false))

      when(controller.landingService.cacheMap(any(), any(), any()))
        .thenReturn(Future.successful(Some(cacheMap)))

      when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val statusResponse = mock[ReadStatusResponse]
      when(statusResponse.safeId) thenReturn Some("X12345678")

      when(controller.statusService.getDetailedStatus(any(), any(), any()))
        .thenReturn(Future.successful((NotCompleted, Some(statusResponse))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("panel-indent").first().child(1).html() must be("Test Company")
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

      "application status is SubmissionReadyForReview" when {
        "there is a fee response available" in new Fixture {
          val paymentRef = paymentRefGen.sample.get
          val payment = paymentGen.sample.get.copy(isBacs = Some(true))

          when(controller.landingService.cacheMap(any(), any(), any()))
            .thenReturn(Future.successful(Some(cacheMap)))

          when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
            .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

          when(controller.statusService.getDetailedStatus(any(), any(), any()))
            .thenReturn(Future.successful((SubmissionReadyForReview, None)))

          when(controller.amlsConnector.getPaymentByAmlsReference(eqTo(amlsRegistrationNumber))(any(), any(), any()))
            .thenReturn(Future.successful(Some(payment)))

          val result = controller.get()(request)
          status(result) must be(OK)

          verify(controller.amlsConnector).getPaymentByAmlsReference(eqTo(amlsRegistrationNumber))(any(), any(), any())

          contentAsString(result) must include(Messages("status.submissionreadyforreview.description"))
        }

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

          contentAsString(result) must include(Messages("status.submissionreadyforreview.nofee.description"))
        }
      }

      "application status is SubmissionDecisionApproved" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
          Some(LocalDate.now.plusDays(30)), false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionApproved, Some(readStatusResponse))))

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

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionRejected, None)))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecision.not.supervised.heading"))

        val html = Jsoup.parse(contentAsString(result))
        Option(html.getElementById("new.application.button")) mustBe defined
      }

      "application status is SubmissionDecisionRevoked and the submit button is allowed" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionRevoked, None)))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecision.not.supervised.heading"))

        val html = Jsoup.parse(contentAsString(result))
        Option(html.getElementById("new.application.button")) mustBe defined

      }

      "application status is SubmissionDecisionExpired" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionExpired, None)))

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

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionWithdrawn, None)))

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

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((DeRegistered, None)))

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

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((RenewalSubmitted(Some(LocalDate.now)), None)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val html = contentAsString(result)
        html must include(Messages("status.renewalsubmitted.description"))

        val doc = Jsoup.parse(html)

        doc.select(s"a[href=${controllers.changeofficer.routes.StillEmployedController.get().url}]").text mustBe Messages("changeofficer.changelink.text")

      }

      "application status is ReadyForRenewal, and the renewal has not been started" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        val renewalDate = LocalDate.now().plusDays(15)

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
          Some(renewalDate), false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

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

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        when(controller.renewalService.isRenewalComplete(any())(any(), any(), any()))
          .thenReturn(Future.successful(false))

        val renewalDate = LocalDate.now().plusDays(15)

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
          Some(renewalDate), false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

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
            msbServices = Some(BusinessMatchingMsbServices(Set(CurrencyExchange))),
            reviewDetails = Some(ReviewDetails("BusinessName", None, mock[Address], "safeId", None))
          )))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        val dataCache = mock[DataCacheConnector]

        when(dataCache.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(controller.renewalService.isRenewalComplete(any())(any(), any(), any()))
          .thenReturn(Future.successful(true))

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        val renewalDate = LocalDate.now().plusDays(15)

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
          Some(renewalDate), false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

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

        val html = contentAsString(result)
        html must include(Messages("status.renewalnotsubmitted.description"))

        val doc = Jsoup.parse(html)
        doc.select(s"a[href=${controllers.changeofficer.routes.StillEmployedController.get().url}]").text mustBe Messages("changeofficer.changelink.text")

      }
    }

    "show the withdrawal link" when {
      "the status is 'ready for review'" in new Fixture {
        val reviewDetails = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

        val statusResponse = mock[ReadStatusResponse]
        when(statusResponse.processingDate).thenReturn(LocalDateTime.now)
        when(statusResponse.safeId).thenReturn(None)

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

        doc.select(s"a[href=${controllers.withdrawal.routes.WithdrawApplicationController.get().url}]").text mustBe Messages("status.withdraw.link-text")
      }
    }

    "show the deregister link" when {
      "the status is 'approved'" in new Fixture {
        val reviewDetails = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "XE0001234567890")

        val statusResponse = mock[ReadStatusResponse]
        when(statusResponse.currentRegYearEndDate).thenReturn(LocalDate.now.some)
        when(statusResponse.safeId).thenReturn(None)

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

        doc.select(s"a[href=${controllers.deregister.routes.DeRegisterApplicationController.get().url}]").text mustBe Messages("status.deregister.link-text")
      }
    }

    "show the change officer link" when {
      "application status is SubmissionDecisionApproved" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
          Some(LocalDate.now.plusDays(30)), false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((SubmissionDecisionApproved, Some(readStatusResponse))))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecisionsupervised.heading"))
        contentAsString(result) mustNot include(Messages("status.submissiondecisionsupervised.renewal.btn"))

        val doc = Jsoup.parse(contentAsString(result))

        doc.select(s"a[href=${controllers.changeofficer.routes.StillEmployedController.get().url}]").asScala foreach {
          _.text mustBe Messages("changeofficer.changelink.text")
        }

      }

      "application status is ReadyForRenewal" in new Fixture {

        when(controller.landingService.cacheMap(any(), any(), any()))
          .thenReturn(Future.successful(Some(cacheMap)))

        when(cacheMap.getEntry[BusinessMatching](Matchers.contains(BusinessMatching.key))(any()))
          .thenReturn(Some(BusinessMatching(Some(reviewDetails), None)))

        when(cacheMap.getEntry[SubscriptionResponse](Matchers.contains(SubscriptionResponse.key))(any()))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 0, None, None, 0, None, 0)))))

        when(authConnector.currentAuthority(any(), any()))
          .thenReturn(Future.successful(Some(authority.copy(enrolments = Some("bar")))))

        val renewalDate = LocalDate.now().plusDays(15)

        val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None,
          Some(renewalDate), false)

        when(controller.statusService.getDetailedStatus(any(), any(), any()))
          .thenReturn(Future.successful((ReadyForRenewal(Some(renewalDate)), Some(readStatusResponse))))

        when(controller.renewalService.getRenewal(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("status.submissiondecisionsupervised.renewal.btn"))

        val doc = Jsoup.parse(contentAsString(result))

        doc.select(s"a[href=${controllers.changeofficer.routes.StillEmployedController.get().url}]").asScala foreach {
          _.text mustBe Messages("changeofficer.changelink.text")
        }
      }
    }
  }
}
