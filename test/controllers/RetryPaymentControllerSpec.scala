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

import config.ApplicationConfig
import connectors._
import controllers.actions.SuccessfulAuthAction
import generators.submission.SubscriptionResponseGenerator
import generators.{AmlsReferenceNumberGenerator, PaymentGenerator}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails.{BusinessDetails, PreviouslyRegisteredNo}
import models.businessmatching.BusinessMatching
import models.payments._
import models.registrationdetails.RegistrationDetails
import models.status._
import models.{status => _, _}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.HttpResponse
import services.cache.Cache
import utils.AmlsSpec

import java.time.LocalDateTime
import scala.concurrent.Future

// scalastyle:off magic.number
class RetryPaymentControllerSpec
    extends AmlsSpec
    with AmlsReferenceNumberGenerator
    with PaymentGenerator
    with SubscriptionResponseGenerator {

  trait Fixture {
    self =>
    val baseUrl    = "http://localhost"
    val request    = addToken(authRequest)
    val controller = new RetryPaymentController(
      SuccessfulAuthAction,
      statusService = mock[StatusService],
      dataCacheConnector = mock[DataCacheConnector],
      amlsConnector = mock[AmlsConnector],
      paymentsService = mock[PaymentsService],
      ds = commonDependencies,
      cc = mockMcc
    )

    val response = subscriptionResponseGen(hasFees = true).sample.get

    protected val mockCacheMap      = mock[Cache]
    val companyNameFromCache        = "My Test Company Name From Cache"
    val companyNameFromRegistration = "My Test Company Name From Registration"

    setupBusinessMatching(companyNameFromCache)

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
      controller.dataCacheConnector.fetch[SubmissionRequestStatus](any(), eqTo(SubmissionRequestStatus.key))(any())
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
      LocalDateTime.now
    )

    val businessDetails = BusinessDetails(previouslyRegistered = Some(PreviouslyRegisteredNo))
    when {
      controller.dataCacheConnector.fetch[BusinessDetails](any(), eqTo(BusinessDetails.key))(any())
    } thenReturn Future.successful(Some(businessDetails))

    val applicationConfig = app.injector.instanceOf[ApplicationConfig]

    def paymentsReturnLocation(ref: String) =
      ReturnLocation(controllers.routes.PaymentConfirmationController.paymentConfirmation(ref))(applicationConfig)

    def setupBusinessMatching(companyName: String) = {

      val model = BusinessMatching(
        reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
      )

      when {
        controller.dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any())
      } thenReturn Future.successful(Some(model))

    }

    def setupStatus(status: SubmissionStatus): Unit = {

      when {
        controller.statusService.getStatus(any[Option[String]](), any(), any())(any(), any(), any())
      } thenReturn Future.successful(status)

      val statusResponse = mock[ReadStatusResponse]
      when(statusResponse.safeId) thenReturn safeIdGen.sample

      when {
        controller.statusService.getDetailedStatus(any[Option[String]](), any(), any())(any(), any(), any())
      } thenReturn Future.successful((status, Some(statusResponse)))
    }
  }

  "ConfirmationController" must {

    "allow a payment to be retried" in new Fixture {
      val amountInPence   = 8765
      val postData        = "paymentRef" -> paymentReferenceNumber
      val payment         = paymentGen.sample.get
      val paymentResponse = paymentResponseGen.sample.get

      when {
        controller.amlsConnector.getPaymentByPaymentReference(eqTo(paymentReferenceNumber), any())(any(), any())
      } thenReturn Future.successful(
        Some(payment.copy(reference = paymentReferenceNumber, amountInPence = amountInPence))
      )

      when {
        controller.paymentsService.paymentsUrlOrDefault(any(), any(), any(), any(), any(), any())(any(), any())
      } thenReturn Future.successful(paymentResponse.nextUrl)

      val result = controller.retryPayment()(requestWithUrlEncodedBody(postData))

      val expectedUrl: Option[String] = Some(paymentResponse.nextUrl.value)
      val actualUrl: Option[String]   = redirectLocation(result)

      status(result) mustBe SEE_OTHER
      actualUrl mustEqual expectedUrl
    }

    "fail if a payment cannot be retried" in new Fixture {

      val postData = "paymentRef" -> paymentReferenceNumber

      when {
        controller.amlsConnector.getPaymentByPaymentReference(eqTo(paymentReferenceNumber), any())(any(), any())
      } thenReturn Future.successful(None)

      val result = controller.retryPayment()(requestWithUrlEncodedBody(postData))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
