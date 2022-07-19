/*
 * Copyright 2022 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors._
import controllers.actions.SuccessfulAuthAction
import generators.submission.SubscriptionResponseGenerator
import generators.{AmlsReferenceNumberGenerator, PaymentGenerator}
import models.ResponseType.SubscriptionResponseType
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails.{BusinessDetails, PreviouslyRegisteredNo, PreviouslyRegisteredYes}
import models.businessmatching.BusinessMatching
import models.payments.PaymentStatuses.{Cancelled, Created, Failed}
import models.payments._
import models.registrationdetails.RegistrationDetails
import models.renewal.{InvolvedInOtherNo, Renewal}
import models.status.{SubmissionDecisionApproved, _}
import models.{status => _, _}
import org.joda.time.{DateTime, LocalDate}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{AmlsSpec, FeeHelper}
import views.html.confirmation._

import scala.concurrent.Future

// scalastyle:off magic.number
class PaymentConfirmationControllerSpec extends AmlsSpec
  with AmlsReferenceNumberGenerator
  with PaymentGenerator
  with SubscriptionResponseGenerator {

  trait Fixture {
    self =>
    val baseUrl = "http://localhost"
    val request = addToken(authRequest(uri = baseUrl))
    lazy val view1 = app.injector.instanceOf[payment_confirmation_renewal]
    lazy val view2 = app.injector.instanceOf[payment_confirmation_amendvariation]
    lazy val view3 = app.injector.instanceOf[payment_confirmation_transitional_renewal]
    lazy val view4 = app.injector.instanceOf[payment_confirmation]
    lazy val view5 = app.injector.instanceOf[payment_failure]
    val controller = new PaymentConfirmationController(
      authAction = SuccessfulAuthAction,
      statusService = mock[StatusService],
      dataCacheConnector = mock[DataCacheConnector],
      amlsConnector = mock[AmlsConnector],
      enrolmentService = mock[AuthEnrolmentsService],
      feeResponseService = mock[FeeResponseService],
      auditConnector = mock[AuditConnector],
      ds = commonDependencies,
      cc = mockMcc,
      feeHelper = mock[FeeHelper],
      payment_confirmation_renewal = view1,
      payment_confirmation_amendvariation = view2,
      payment_confirmation_transitional_renewal = view3,
      payment_confirmation = view4,
      payment_failure = view5)

    val amlsRegistrationNumber = "amlsRefNumber"

    val response = subscriptionResponseGen(hasFees = true).sample.get

    protected val mockCacheMap = mock[CacheMap]
    val companyNameFromCache = "My Test Company Name From Cache"
    val companyNameFromRegistration = "My Test Company Name From Registration"

    setupBusinessMatching(companyNameFromCache)

    when {
      controller.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(mock[AuditResult])

    when {
      controller.amlsConnector.refreshPaymentStatus(any(), any())(any(), any())
    } thenReturn Future.successful(paymentStatusResultGen.sample.get.copy(currentStatus = PaymentStatuses.Successful))

    when {
      controller.amlsConnector.getPaymentByPaymentReference(any(), any())(any(), any())
    } thenReturn Future.successful(paymentGen.sample)

    when {
      controller.amlsConnector.savePayment(any(), any(), any(), any())(any(), any())
    } thenReturn Future.successful(HttpResponse(CREATED, ""))

    when {
      controller.amlsConnector.registrationDetails(any(), any())(any(), any())
    } thenReturn Future.successful(RegistrationDetails(companyNameFromRegistration, isIndividual = false))

    when {
      controller.dataCacheConnector.fetch[SubmissionRequestStatus](any(), eqTo(SubmissionRequestStatus.key))(any(), any())
    } thenReturn Future.successful(Some(SubmissionRequestStatus(true)))

    def feeResponse(responseType: ResponseType) = FeeResponse(
      responseType = responseType,
      amlsReferenceNumber = amlsRegistrationNumber,
      registrationFee = 100,
      fpFee = None,
      approvalCheckFee = None,
      premiseFee = 0,
      totalFees = 200,
      paymentReference = Some(paymentReferenceNumber),
      difference = Some(115),
      createdAt = DateTime.now
    )

    when {
      controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any(), any())
    } thenReturn Future.successful(Some(feeResponse(SubscriptionResponseType)))

    val businessDetails = BusinessDetails(previouslyRegistered = Some(PreviouslyRegisteredNo))
    when {
      controller.dataCacheConnector.fetch[BusinessDetails](any(), eqTo(BusinessDetails.key))(any(), any())
    } thenReturn Future.successful(Some(businessDetails))

    val applicationConfig = app.injector.instanceOf[ApplicationConfig]

    def paymentsReturnLocation(ref: String) = ReturnLocation(controllers.routes.PaymentConfirmationController.paymentConfirmation(ref))(applicationConfig)

    def setupBusinessMatching(companyName: String) = {

      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any(), any())
      } thenReturn Future.successful(Some(model))

    }

    def setupStatus(status: SubmissionStatus): Unit = {

      when {
        controller.statusService.getStatus(any[Option[String]](), any(), any())(any(), any())
      } thenReturn Future.successful(status)

      val statusResponse = mock[ReadStatusResponse]
      when(statusResponse.safeId) thenReturn safeIdGen.sample

      when {
        controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any())
      } thenReturn Future.successful((status, Some(statusResponse)))
    }
  }

  "ConfirmationController" must {

    "show the correct payment confirmation page" when {

      "the application status is 'new submission'" in new Fixture {

        setupStatus(SubmissionReady)

        when {
          controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))(any(), any())
        } thenReturn Future.successful(None)

        val result = controller.paymentConfirmation(paymentReferenceNumber)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.lede")
        doc.select(".confirmation").text must include(paymentReferenceNumber)
        doc.select(".confirmation").text must include(companyNameFromRegistration)
      }

      "the application status is 'new submission' and has been previously registered" in new Fixture {

        setupStatus(SubmissionReady)

        when {
          controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))(any(), any())
        } thenReturn Future.successful(None)


        val businessDetailsYes = BusinessDetails(previouslyRegistered = Some(PreviouslyRegisteredYes(Some("123456"))))

        when {
          controller.dataCacheConnector.fetch[BusinessDetails](any(), eqTo(BusinessDetails.key))(any(), any())
        } thenReturn Future.successful(Some(businessDetailsYes))

        val result = controller.paymentConfirmation(paymentReferenceNumber)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.html() must include(Messages("confirmation.payment.info.hmrc.review.1"))
        doc.html() must include(Messages("confirmation.payment.info.hmrc.review.2"))
        doc.html() must include(Messages("confirmation.payment.info.hmrc.review.3"))
      }

      "the application status is 'pending'" in new Fixture {

        setupStatus(SubmissionReadyForReview)

        when {
          controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))(any(), any())
        } thenReturn Future.successful(None)

        val result = controller.paymentConfirmation(paymentReferenceNumber)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.payment.amendvariation.title"))
        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.amendvariation.lede")
        doc.select(".confirmation").text must include(paymentReferenceNumber)
        doc.select(".confirmation").text must include(companyNameFromRegistration)
        contentAsString(result) must include(Messages("confirmation.payment.amendvariation.info.keep_up_to_date"))
      }

      "the application status is 'approved'" in new Fixture {

        setupStatus(SubmissionDecisionApproved)

        when {
          controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))(any(), any())
        } thenReturn Future.successful(None)

        val result = controller.paymentConfirmation(paymentReferenceNumber)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.payment.amendvariation.title"))
        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.amendvariation.lede")
        doc.select(".confirmation").text must include(paymentReferenceNumber)
        doc.select(".confirmation").text must include(companyNameFromRegistration)
        contentAsString(result) must include(Messages("confirmation.payment.amendvariation.info.keep_up_to_date"))
      }

      "the application status is 'Renewal Submitted'" in new Fixture {

        setupStatus(RenewalSubmitted(None))

        when {
          controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))(any(), any())
        } thenReturn Future.successful(None)

        val result = controller.paymentConfirmation(paymentReferenceNumber)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.payment.amendvariation.title"))
        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.amendvariation.lede")
        doc.select(".confirmation").text must include(paymentReferenceNumber)
        doc.select(".confirmation").text must include(companyNameFromRegistration)
        contentAsString(result) must include(Messages("confirmation.payment.amendvariation.info.keep_up_to_date"))
      }

      "the application status is 'ready for renewal'" in new Fixture {

        setupStatus(ReadyForRenewal(Some(new LocalDate())))

        when {
          controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))(any(), any())
        }.thenReturn(Future.successful(Some(Renewal(Some(InvolvedInOtherNo)))))

        val result = controller.paymentConfirmation(paymentReferenceNumber)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.payment.renewal.title"))
        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.renewal.lede")
        doc.select(".confirmation").text must include(paymentReferenceNumber)
        doc.select(".confirmation").text must include(companyNameFromRegistration)
        contentAsString(result) must include(Messages("confirmation.payment.amendvariation.info.keep_up_to_date"))
      }

      "the application status is 'ready for renewal' and user has done only variation" in new Fixture {

        setupStatus(ReadyForRenewal(Some(new LocalDate())))

        when {
          controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))(any(), any())
        } thenReturn Future.successful(None)

        val result = controller.paymentConfirmation(paymentReferenceNumber)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.payment.amendvariation.title"))
        doc.select("h1.heading-large").text mustBe Messages("confirmation.payment.amendvariation.lede")
        doc.select(".confirmation").text must include(paymentReferenceNumber)
        doc.select(".confirmation").text must include(companyNameFromRegistration)
        contentAsString(result) must include(Messages("confirmation.payment.amendvariation.info.keep_up_to_date"))
      }

      "the payment failed" in new Fixture {

        setupStatus(SubmissionReadyForReview)

        when {
          controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))(any(), any())
        } thenReturn Future.successful(None)

        val payment = paymentGen.sample.get.copy(status = Failed)
        val paymentStatus = paymentStatusResultGen.sample.get.copy(currentStatus = payment.status)

        when {
          controller.amlsConnector.refreshPaymentStatus(any(), any())(any(), any())
        } thenReturn Future.successful(paymentStatus)

        val failedRequest = addToken(FakeRequest("GET", s"$baseUrl?paymentStatus=Failed"))

        val result = controller.paymentConfirmation(payment.reference)(failedRequest)

        status(result) mustBe OK

        verify(controller.amlsConnector).refreshPaymentStatus(eqTo(payment.reference), eqTo(("accType", "id")))(any(), any())
        contentAsString(result) must include(Messages("confirmation.payment.failed.header"))
        contentAsString(result) must include(Messages("confirmation.payment.failed.reason.failure"))
      }

      "the payment was cancelled" in new Fixture {

        setupStatus(SubmissionReadyForReview)

        when {
          controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))(any(), any())
        } thenReturn Future.successful(None)

        val payment = paymentGen.sample.get.copy(status = Cancelled)
        val paymentStatus = paymentStatusResultGen.sample.get.copy(currentStatus = payment.status)

        when {
          controller.amlsConnector.refreshPaymentStatus(any(), any())(any(), any())
        } thenReturn Future.successful(paymentStatus)

        val cancelledRequest = addToken(FakeRequest("GET", s"$baseUrl?paymentStatus=Cancelled"))
        val result = controller.paymentConfirmation(payment.reference)(cancelledRequest)

        status(result) mustBe OK

        verify(controller.amlsConnector).refreshPaymentStatus(eqTo(payment.reference), eqTo(("accType", "id")))(any(), any())
        contentAsString(result) must include(Messages("confirmation.payment.failed.header"))
        contentAsString(result) must include(Messages("confirmation.payment.failed.reason.cancelled"))
      }

      "payment data says 'Created' but querystring says 'Cancelled'" in new Fixture {
        setupStatus(SubmissionReadyForReview)

        when {
          controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))(any(), any())
        } thenReturn Future.successful(None)

        val payment = paymentGen.sample.get.copy(status = Created)
        val paymentStatus = paymentStatusResultGen.sample.get.copy(currentStatus = payment.status)

        when {
          controller.amlsConnector.refreshPaymentStatus(any(), any())(any(), any())
        } thenReturn Future.successful(paymentStatus)

        when {
          controller.amlsConnector.getPaymentByAmlsReference(any(), any())(any(), any())
        } thenReturn Future.successful(Some(payment))

        val cancelledRequest = addToken(FakeRequest("GET", s"$baseUrl?paymentStatus=Cancelled"))
        val result = controller.paymentConfirmation(payment.reference)(cancelledRequest)

        status(result) mustBe OK

        verify(controller.amlsConnector).refreshPaymentStatus(eqTo(payment.reference), eqTo(("accType", "id")))(any(), any())
        contentAsString(result) must include(Messages("confirmation.payment.failed.header"))
        contentAsString(result) must include(Messages("confirmation.payment.failed.reason.cancelled"))
      }
    }
  }
}