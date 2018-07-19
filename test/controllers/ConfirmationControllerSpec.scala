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

import connectors._
import generators.submission.SubscriptionResponseGenerator
import generators.{AmlsReferenceNumberGenerator, PaymentGenerator}
import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import models.aboutthebusiness.{AboutTheBusiness, PreviouslyRegisteredNo, PreviouslyRegisteredYes}
import models.businesscustomer.{Address, ReviewDetails}
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
class ConfirmationControllerSpec extends AmlsSpec
  with AmlsReferenceNumberGenerator
  with PaymentGenerator
  with SubscriptionResponseGenerator {

  trait Fixture extends AuthorisedFixture {
    self =>
    val baseUrl = "http://localhost"
    val request = addToken(authRequest).copyFakeRequest(uri = baseUrl)

    val controller = new ConfirmationController(
      keystoreConnector = mock[KeystoreConnector],
      authConnector = self.authConnector,
      statusService = mock[StatusService],
      dataCacheConnector = mock[DataCacheConnector],
      amlsConnector = mock[AmlsConnector],
      authEnrolmentsService = mock[AuthEnrolmentsService],
      feeResponseService = mock[FeeResponseService],
      paymentsConnector = mock[PayApiConnector],
      confirmationService = mock[ConfirmationService],
      paymentsService = mock[PaymentsService],
      auditConnector = mock[AuditConnector]
    )

    val response = subscriptionResponseGen(hasFees = true).sample.get

    protected val mockCacheMap = mock[CacheMap]
    val companyName = "My Test Company"

    setupBusinessMatching(companyName)

    when {
      controller.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(mock[AuditResult])

    when {
      controller.keystoreConnector.setConfirmationStatus(any(), any())
    } thenReturn Future.successful()

    when {
      controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
    } thenReturn Future.successful(Some(amlsRegistrationNumber))

    when {
      controller.paymentsConnector.createPayment(any())(any(), any())
    } thenReturn Future.successful(Some(CreatePaymentResponse(PayApiLinks("/payments"), Some(amlsRegistrationNumber))))

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
    } thenReturn Future.successful(RegistrationDetails(companyName, isIndividual = false))

    when {
      controller.dataCacheConnector.fetch[SubmissionRequestStatus](eqTo(SubmissionRequestStatus.key))(any(),any(),any())
    } thenReturn Future.successful(Some(SubmissionRequestStatus(true)))

    def feeResponse(responseType: ResponseType) = FeeResponse(
      responseType,
      amlsRegistrationNumber,
      100,
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

    when {
      controller.confirmationService.getBreakdownRows(any(), any())(any(), any(), any())
    } thenReturn Future.successful(Some(breakdownRows))

    val aboutTheBusiness = AboutTheBusiness(previouslyRegistered = Some(PreviouslyRegisteredNo))
    when {
      controller.dataCacheConnector.fetch[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any(), any(), any())
    } thenReturn Future.successful(Some(aboutTheBusiness))

    def paymentsReturnLocation(ref: String) = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(ref))

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

    "write a confirmation value to Keystore" in new Fixture {

      val submissionStatus = SubmissionReady

      setupStatus(submissionStatus)

      when {
        controller.confirmationService.getBreakdownRows(eqTo(SubmissionReady), any())(any(), any(), any())
      } thenReturn Future.successful(Some(Seq.empty))

      val result = controller.get()(request)

      status(result) mustBe OK

      verify(controller.keystoreConnector).setConfirmationStatus(any(), any())

    }

    "notify user of progress if application has not already been submitted" in new Fixture {

      val submissionStatus = SubmissionReady

      setupStatus(submissionStatus)

      when {
        controller.confirmationService.getBreakdownRows(eqTo(SubmissionReady), any())(any(), any(), any())
      } thenReturn Future.successful(Some(Seq.empty))

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("Application fee and reference")
      contentAsString(result) must include(paymentReferenceNumber)
    }

    "notify the user that there is a fee" when {
      "submitting a new application" which {

        "has response data" in new Fixture {
          setupStatus(SubmissionReadyForReview)

          val fees = feeResponse(SubscriptionResponseType)
          val rows = Gen.listOfN(5, breakdownRowGen).sample

          when {
            controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(), any(), any())
          } thenReturn Future.successful(Some(fees))

          when {
            controller.confirmationService.getBreakdownRows(eqTo(SubmissionReadyForReview), eqTo(fees))(any(), any(), any())
          } thenReturn Future.successful(rows)

          val result = controller.get()(request)
          status(result) mustBe OK

          val doc = Jsoup.parse(contentAsString(result))

          doc.title must include(Messages("confirmation.header"))
          contentAsString(result) must include(Messages("confirmation.submission.info"))
          contentAsString(result) must include(Messages("confirmation.breakdown.details"))
        }

        "does not have response data" in new Fixture {
          setupStatus(SubmissionReadyForReview)

          val fees = feeResponse(SubscriptionResponseType)

          when {
            controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(), any(), any())
          } thenReturn Future.successful(Some(fees))

          when {
            controller.confirmationService.getBreakdownRows(eqTo(SubmissionReadyForReview), eqTo(fees))(any(), any(), any())
          } thenReturn Future.successful(None)

          val result = controller.get()(request)
          status(result) mustBe OK

          val doc = Jsoup.parse(contentAsString(result))

          doc.title must include(Messages("confirmation.header"))
          contentAsString(result) must include(Messages("confirmation.submission.info"))
          contentAsString(result) must not include Messages("confirmation.breakdown.details")
        }
      }

      "submitting an amendment or variation" which {

        "has response data" in new Fixture {
          setupStatus(SubmissionReadyForReview)

          val fees = feeResponse(AmendOrVariationResponseType)
          val rows = Gen.listOfN(5, breakdownRowGen).sample

          when {
            controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(), any(), any())
          } thenReturn Future.successful(Some(fees))

          when {
            controller.confirmationService.getBreakdownRows(eqTo(SubmissionReadyForReview), eqTo(fees))(any(), any(), any())
          } thenReturn Future.successful(rows)

          val result = controller.get()(request)
          status(result) mustBe OK

          val doc = Jsoup.parse(contentAsString(result))

          doc.title must include(Messages("confirmation.amendment.header"))
          contentAsString(result) must include(Messages("confirmation.amendment.info"))
          contentAsString(result) must include(Messages("confirmation.breakdown.details"))
        }

        "does not have response data" in new Fixture {
          setupStatus(SubmissionReadyForReview)

          val fees = feeResponse(AmendOrVariationResponseType)

          when {
            controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(), any(), any())
          } thenReturn Future.successful(Some(fees))

          when {
            controller.confirmationService.getBreakdownRows(eqTo(SubmissionReadyForReview), eqTo(fees))(any(), any(), any())
          } thenReturn Future.successful(None)

          val result = controller.get()(request)
          status(result) mustBe OK

          val doc = Jsoup.parse(contentAsString(result))

          doc.title must include(Messages("confirmation.amendment.header"))
          contentAsString(result) must include(Messages("confirmation.amendment.info"))
          contentAsString(result) must not include Messages("confirmation.breakdown.details")
        }
      }

      "submitting a variation" which {
        "has no difference, but has a total fee value" in new Fixture {
          setupStatus(SubmissionDecisionApproved)

          when {
            controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(), any(), any())
          } thenReturn Future.successful(Some(feeResponse(AmendOrVariationResponseType)))

          val result = controller.get()(request)
          status(result) mustBe OK

          val doc = Jsoup.parse(contentAsString(result))

          doc.title must include(Messages("confirmation.amendment.header"))
          contentAsString(result) must include(Messages("confirmation.amendment.info"))
        }
      }

      "submitting a renewal" in new Fixture {
        setupStatus(RenewalSubmitted(None))

        when {
          controller.confirmationService.isRenewalDefined(any(), any(), any())
        } thenReturn Future.successful(true)

        when {
          controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(), any(), any())
        } thenReturn Future.successful(Some(feeResponse(AmendOrVariationResponseType)))

        val result = controller.get()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.title must include(Messages("confirmation.renewal.title"))
        contentAsString(result) must include(Messages("confirmation.renewal.header"))
        doc.select("#fee").text must include(Currency(200d).toString)
      }
    }

    "notify user there is no fee" when {

      "has no fee response" in new Fixture {

        setupStatus(SubmissionReadyForReview)

        when {
          controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(), any(), any())
        } thenReturn Future.successful(None)

        when {
          controller.confirmationService.getBreakdownRows(eqTo(SubmissionReadyForReview), any())(any(), any(), any())
        } thenReturn Future.successful(Some(Seq.empty))

        val result = controller.get()(request)
        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include(Messages("confirmation.variation.title"))
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }

      "has no payment reference" in new Fixture {

        setupStatus(SubmissionDecisionApproved)

        when {
          controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(), any(), any())
        } thenReturn Future.successful(Some(feeResponse(SubscriptionResponseType).copy(paymentReference = None)))

        when {
          controller.confirmationService.getBreakdownRows(eqTo(SubmissionDecisionApproved), any())(any(), any(), any())
        } thenReturn Future.successful(Some(Seq.empty))

        val result = controller.get()(request)
        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include(Messages("confirmation.variation.title"))
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyName)
      }
    }

    "allow a payment to be retried" in new Fixture {
      val amountInPence = 8765
      val postData = "paymentRef" -> paymentReferenceNumber
      val payment = paymentGen.sample.get
      val paymentResponse = paymentResponseGen.sample.get

      when {
        controller.amlsConnector.getPaymentByPaymentReference(eqTo(paymentReferenceNumber))(any(), any(), any())
      } thenReturn Future.successful(Some(payment.copy(reference = paymentReferenceNumber, amountInPence = amountInPence)))

      when {
        controller.paymentsService.paymentsUrlOrDefault(any(), any(), any(), any(), any())(any(), any(), any(), any())
      } thenReturn Future.successful(paymentResponse)

      val result = controller.retryPayment()(request.withFormUrlEncodedBody(postData))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(paymentResponse.links.nextUrl)
    }

    "fail if a payment cannot be retried" in new Fixture {

      val postData = "paymentRef" -> paymentReferenceNumber

      when {
        controller.amlsConnector.getPaymentByPaymentReference(eqTo(paymentReferenceNumber))(any(), any(), any())
      } thenReturn Future.successful(None)

      val result = controller.retryPayment()(request.withFormUrlEncodedBody(postData))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

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
        doc.select(".confirmation").text must include(companyName)
      }

      "the application status is 'new submission' and has been previously registered" in new Fixture {

        setupStatus(SubmissionReady)

        when {
          controller.dataCacheConnector.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(None)


        val aboutTheBusinessYes = AboutTheBusiness(previouslyRegistered = Some(PreviouslyRegisteredYes("123456")))

        when {
          controller.dataCacheConnector.fetch[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any(), any(), any())
        } thenReturn Future.successful(Some(aboutTheBusinessYes))

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
        doc.select(".confirmation").text must include(companyName)
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
        doc.select(".confirmation").text must include(companyName)
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
        doc.select(".confirmation").text must include(companyName)
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
        doc.select(".confirmation").text must include(companyName)
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
        doc.select(".confirmation").text must include(companyName)
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

      "bacs confirmation is requested" in new Fixture {

        when {
          controller.statusService.getReadStatus(any())(any(), any(), any())
        } thenReturn Future.successful(ReadStatusResponse(LocalDateTime.now(), "", None, None, None, None, false))

        val result = controller.bacsConfirmation()(request)

        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).select("h1.heading-large").text must include(Messages("confirmation.payment.bacs.header"))

      }

      "bacs confirmation is requested and is a transitional renewal" in new Fixture {

        val aboutTheBusinessYes = AboutTheBusiness(previouslyRegistered = Some(PreviouslyRegisteredYes("123456")))

        when {
          controller.statusService.getReadStatus(any())(any(), any(), any())
        } thenReturn Future.successful(ReadStatusResponse(LocalDateTime.now(), "", None, None, None, None, false))

        when {
          controller.dataCacheConnector.fetch[AboutTheBusiness](eqTo(AboutTheBusiness.key))(any(), any(), any())
        } thenReturn Future.successful(Some(aboutTheBusinessYes))

        val result = controller.bacsConfirmation()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))

        doc.html() must include(Messages("confirmation.payment.info.transitional.renewal.hmrc_review"))
        doc.html() must include(Messages("confirmation.payment.info.transitional.renewal.hmrc_review2"))
      }
    }
  }
}
