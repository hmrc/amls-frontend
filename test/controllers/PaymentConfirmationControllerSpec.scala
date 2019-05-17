/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors._
import generators.submission.SubscriptionResponseGenerator
import generators.{AmlsReferenceNumberGenerator, PaymentGenerator}
import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails.{BusinessDetails, PreviouslyRegisteredNo, PreviouslyRegisteredYes}
import models.businessmatching.BusinessMatching
import models.confirmation.{BreakdownRow, Currency}
import models.payments.PaymentStatuses.{Cancelled, Created, Failed}
import models.payments._
import models.registrationdetails.RegistrationDetails
import models.renewal.{InvolvedInOtherNo, Renewal}
import models.status.{SubmissionDecisionApproved, _}
import models.{status => _, _}
import org.joda.time.{DateTime, LocalDate, LocalDateTime}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Gen
import play.api.i18n.Messages
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

// scalastyle:off magic.number
class PaymentConfirmationControllerSpec extends AmlsSpec
  with AmlsReferenceNumberGenerator
  with PaymentGenerator
  with SubscriptionResponseGenerator {

  trait Fixture extends AuthorisedFixture {
    self =>
    val baseUrl = "http://localhost"
    val request = addToken(authRequest).copyFakeRequest(uri = baseUrl)

    val controller = new PaymentConfirmationController(
      authConnector = self.authConnector,
      statusService = mock[StatusService],
      dataCacheConnector = mock[DataCacheConnector],
      amlsConnector = mock[AmlsConnector],
      authEnrolmentsService = mock[AuthEnrolmentsService],
      feeResponseService = mock[FeeResponseService],
      auditConnector = mock[AuditConnector]
    )

    val response = subscriptionResponseGen(hasFees = true).sample.get

    protected val mockCacheMap = mock[CacheMap]
    val companyNameFromCache = "My Test Company Name From Cache"
    val companyNameFromRegistration = "My Test Company Name From Registration"

    setupBusinessMatching(companyNameFromCache)

    when {
      controller.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(mock[AuditResult])

    when {
      controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
    } thenReturn Future.successful(Some(amlsRegistrationNumber))

    when {
      controller.amlsConnector.refreshPaymentStatus(any())(any(), any(), any())
    } thenReturn Future.successful(paymentStatusResultGen.sample.get.copy(currentStatus = PaymentStatuses.Successful))

    when {
      controller.amlsConnector.getPaymentByPaymentReference(any())(any(), any(), any())
    } thenReturn Future.successful(paymentGen.sample)

    when {
      controller.amlsConnector.savePayment(any(), any(), any())(any(), any(), any())
    } thenReturn Future.successful(HttpResponse(CREATED))

    when {
      controller.amlsConnector.registrationDetails(any())(any(), any(), any())
    } thenReturn Future.successful(RegistrationDetails(companyNameFromRegistration, isIndividual = false))

    when {
      controller.dataCacheConnector.fetch[SubmissionRequestStatus](eqTo(SubmissionRequestStatus.key))(any(),any(),any())
    } thenReturn Future.successful(Some(SubmissionRequestStatus(true)))

    def feeResponse(responseType: ResponseType) = FeeResponse(
      responseType,
      amlsRegistrationNumber,
      100,
      None,
      None,
      0,
      200,
      Some(paymentReferenceNumber),
      Some(115),
      DateTime.now
    )

    when {
      controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(), any(), any())
    } thenReturn Future.successful(Some(feeResponse(SubscriptionResponseType)))

    val breakdownRows = Seq.empty[BreakdownRow]

    val businessDetails = BusinessDetails(previouslyRegistered = Some(PreviouslyRegisteredNo))
    when {
      controller.dataCacheConnector.fetch[BusinessDetails](eqTo(BusinessDetails.key))(any(), any(), any())
    } thenReturn Future.successful(Some(businessDetails))

    def paymentsReturnLocation(ref: String) = ReturnLocation(controllers.routes.PaymentConfirmationController.paymentConfirmation(ref))

    def setupBusinessMatching(companyName: String) = {

      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      } thenReturn Future.successful(Some(model))

    }

    def setupStatus(status: SubmissionStatus): Unit = {

      when {
        controller.statusService.getStatus(any(), any(), any())
      } thenReturn Future.successful(status)

      val statusResponse = mock[ReadStatusResponse]
      when(statusResponse.safeId) thenReturn safeIdGen.sample

      when {
        controller.statusService.getDetailedStatus(any(), any(), any())
      } thenReturn Future.successful((status, Some(statusResponse)))
    }
  }

  "ConfirmationController" must {

    "show the correct payment confirmation page" when {

      "the application status is 'new submission'" in new Fixture {

        setupStatus(SubmissionReady)

        when {
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
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
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(None)


        val businessDetailsYes = BusinessDetails(previouslyRegistered = Some(PreviouslyRegisteredYes("123456")))

        when {
          controller.dataCacheConnector.fetch[BusinessDetails](eqTo(BusinessDetails.key))(any(), any(), any())
        } thenReturn Future.successful(Some(businessDetailsYes))

        val result = controller.paymentConfirmation(paymentReferenceNumber)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.html() must include(Messages("confirmation.payment.info.transitional.renewal.hmrc_review"))
        doc.html() must include(Messages("confirmation.payment.info.transitional.renewal.hmrc_review2"))
      }

      "the application status is 'pending'" in new Fixture {

        setupStatus(SubmissionReadyForReview)

        when {
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
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
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
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
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
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
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
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
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
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
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(None)

        val payment = paymentGen.sample.get.copy(status = Failed)
        val paymentStatus = paymentStatusResultGen.sample.get.copy(currentStatus = payment.status)

        when {
          controller.amlsConnector.refreshPaymentStatus(any())(any(), any(), any())
        } thenReturn Future.successful(paymentStatus)

        val failedRequest = addToken(authRequest).copyFakeRequest(uri = baseUrl + "?paymentStatus=Failed")
        val result = controller.paymentConfirmation(payment.reference)(failedRequest)

        status(result) mustBe OK

        verify(controller.amlsConnector).refreshPaymentStatus(eqTo(payment.reference))(any(), any(), any())
        contentAsString(result) must include(Messages("confirmation.payment.failed.header"))
        contentAsString(result) must include(Messages("confirmation.payment.failed.reason.failure"))
      }

      "the payment was cancelled" in new Fixture {

        setupStatus(SubmissionReadyForReview)

        when {
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(None)

        val payment = paymentGen.sample.get.copy(status = Cancelled)
        val paymentStatus = paymentStatusResultGen.sample.get.copy(currentStatus = payment.status)

        when {
          controller.amlsConnector.refreshPaymentStatus(any())(any(), any(), any())
        } thenReturn Future.successful(paymentStatus)

        val cancelledRequest = request.copyFakeRequest(uri = baseUrl + "?paymentStatus=Cancelled")
        val result = controller.paymentConfirmation(payment.reference)(cancelledRequest)

        status(result) mustBe OK

        verify(controller.amlsConnector).refreshPaymentStatus(eqTo(payment.reference))(any(), any(), any())
        contentAsString(result) must include(Messages("confirmation.payment.failed.header"))
        contentAsString(result) must include(Messages("confirmation.payment.failed.reason.cancelled"))
      }

      "payment data says 'Created' but querystring says 'Cancelled'" in new Fixture {
        setupStatus(SubmissionReadyForReview)

        when {
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(None)

        val payment = paymentGen.sample.get.copy(status = Created)
        val paymentStatus = paymentStatusResultGen.sample.get.copy(currentStatus = payment.status)

        when {
          controller.amlsConnector.refreshPaymentStatus(any())(any(), any(), any())
        } thenReturn Future.successful(paymentStatus)

        when {
          controller.amlsConnector.getPaymentByAmlsReference(any())(any(), any(), any())
        } thenReturn Future.successful(Some(payment))

        val cancelledRequest = request.copyFakeRequest(uri = baseUrl + "?paymentStatus=Cancelled")
        val result = controller.paymentConfirmation(payment.reference)(cancelledRequest)

        status(result) mustBe OK

        verify(controller.amlsConnector).refreshPaymentStatus(eqTo(payment.reference))(any(), any(), any())
        contentAsString(result) must include(Messages("confirmation.payment.failed.header"))
        contentAsString(result) must include(Messages("confirmation.payment.failed.reason.cancelled"))
      }
    }
  }
}