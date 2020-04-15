/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.actions.SuccessfulAuthAction
import generators.submission.SubscriptionResponseGenerator
import generators.{AmlsReferenceNumberGenerator, PaymentGenerator}
import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails.{BusinessDetails, PreviouslyRegisteredNo}
import models.businessmatching.BusinessMatching
import models.confirmation.Currency
import models.payments._
import models.registrationdetails.RegistrationDetails
import models.status.{SubmissionDecisionApproved, _}
import models.{status => _, _}
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Gen
import play.api.i18n.Messages
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, FeeHelper}

import scala.concurrent.Future

// scalastyle:off magic.number
class ConfirmationControllerSpec extends AmlsSpec
  with AmlsReferenceNumberGenerator
  with PaymentGenerator
  with SubscriptionResponseGenerator {

  trait Fixture {
    self =>
    val baseUrl = "http://localhost"
    val request = addToken(authRequest.copyFakeRequest(uri = baseUrl))

    val controller = new ConfirmationController(
      keystoreConnector = mock[KeystoreConnector],
      authAction = SuccessfulAuthAction,
      statusService = mock[StatusService],
      dataCacheConnector = mock[DataCacheConnector],
      amlsConnector = mock[AmlsConnector],
      ds = commonDependencies,
      enrolmentService = mock[AuthEnrolmentsService],
      authenticator = mock[AuthenticatorConnector],
      confirmationService = mock[ConfirmationService],
      cc = mockMcc,
      feeHelper = mock[FeeHelper])

    val amlsRegistrationNumber = "amlsRefNumber"

    val response = subscriptionResponseGen(hasFees = true).sample.get

    protected val mockCacheMap = mock[CacheMap]
    val companyNameFromCache = "My Test Company Name From Cache"
    val companyNameFromRegistration = "My Test Company Name From Registration"

    setupBusinessMatching(companyNameFromCache)

    when {
      controller.authenticator.refreshProfile(any(), any())
    } thenReturn Future.successful(HttpResponse(OK))

    when {
      controller.keystoreConnector.setConfirmationStatus(any(), any())
    } thenReturn Future.successful()

    when {
      controller.amlsConnector.refreshPaymentStatus(any(), any())(any(), any())
    } thenReturn Future.successful(paymentStatusResultGen.sample.get.copy(currentStatus = PaymentStatuses.Successful))

    when {
      controller.amlsConnector.getPaymentByPaymentReference(any(), any())(any(), any())
    } thenReturn Future.successful(paymentGen.sample)

    when {
      controller.amlsConnector.savePayment(any(), any(), any(), any())(any(), any())
    } thenReturn Future.successful(HttpResponse(CREATED))

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
      controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
    } thenReturn Future.successful(Some(feeResponse(SubscriptionResponseType)))

    val businessDetails = BusinessDetails(previouslyRegistered = Some(PreviouslyRegisteredNo))

    when {
      controller.dataCacheConnector.fetch[BusinessDetails](any(), eqTo(BusinessDetails.key))(any(), any())
    } thenReturn Future.successful(Some(businessDetails))

    def paymentsReturnLocation(ref: String) = ReturnLocation(controllers.routes.PaymentConfirmationController.paymentConfirmation(ref))

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

    "write a confirmation value to Keystore" in new Fixture {

      val submissionStatus = SubmissionReady

      setupStatus(submissionStatus)

      val result = controller.get()(request)

      status(result) mustBe OK

      verify(controller.keystoreConnector).setConfirmationStatus(any(), any())

    }

    "notify user of progress if application has not already been submitted" in new Fixture {

      val submissionStatus = SubmissionReady

      setupStatus(submissionStatus)

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("Your fee and payment reference")
      contentAsString(result) must include(paymentReferenceNumber)
    }

    "notify the user that there is a fee" when {
      "submitting a new application" which {

        "has response data" in new Fixture {
          setupStatus(SubmissionReadyForReview)

          val fees = feeResponse(SubscriptionResponseType)

          when {
            controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
          } thenReturn Future.successful(Some(fees))

          val result = controller.get()(request)
          status(result) mustBe OK

          val doc = Jsoup.parse(contentAsString(result))

          doc.title must include(Messages("confirmation.header"))
          contentAsString(result) must include(Messages("confirmation.submission.info"))
        }

        "does not have response data" in new Fixture {
          setupStatus(SubmissionReadyForReview)

          val fees = feeResponse(SubscriptionResponseType)

          when {
            controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
          } thenReturn Future.successful(Some(fees))

          val result = controller.get()(request)
          status(result) mustBe OK

          val doc = Jsoup.parse(contentAsString(result))

          doc.title must include(Messages("confirmation.header"))
          contentAsString(result) must include(Messages("confirmation.submission.info"))
        }
      }

      "submitting an amendment or variation" which {

        "has response data" in new Fixture {
          setupStatus(SubmissionReadyForReview)

          val fees = feeResponse(AmendOrVariationResponseType)

          when {
            controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
          } thenReturn Future.successful(Some(fees))

          val result = controller.get()(request)
          status(result) mustBe OK

          val doc = Jsoup.parse(contentAsString(result))

          doc.title must include(Messages("confirmation.amendment.header"))
          contentAsString(result) must include(Messages("confirmation.amendment.info"))
        }

        "does not have response data" in new Fixture {
          setupStatus(SubmissionReadyForReview)

          val fees = feeResponse(AmendOrVariationResponseType)

          when {
            controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
          } thenReturn Future.successful(Some(fees))

          val result = controller.get()(request)
          status(result) mustBe OK

          val doc = Jsoup.parse(contentAsString(result))

          doc.title must include(Messages("confirmation.amendment.header"))
          contentAsString(result) must include(Messages("confirmation.amendment.info"))
        }
      }

      "submitting a variation" which {
        "has no difference, but has a total fee value" in new Fixture {
          setupStatus(SubmissionDecisionApproved)

          when {
            controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
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
          controller.confirmationService.isRenewalDefined(any[String]())(any(), any())
        } thenReturn Future.successful(true)

        when {
          controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
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
          controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
        } thenReturn Future.successful(None)

        val result = controller.get()(request)
        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include(Messages("confirmation.variation.title"))
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyNameFromRegistration)
      }

      "has no payment reference" in new Fixture {

        setupStatus(SubmissionDecisionApproved)

        when {
          controller.feeHelper.retrieveFeeResponse(any(), any[(String, String)](), any(), any())(any())
        } thenReturn Future.successful(Some(feeResponse(SubscriptionResponseType).copy(paymentReference = None)))

        val result = controller.get()(request)
        status(result) mustBe OK

        Jsoup.parse(contentAsString(result)).title must include(Messages("confirmation.variation.title"))
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(companyNameFromRegistration)
      }
    }
  }
}