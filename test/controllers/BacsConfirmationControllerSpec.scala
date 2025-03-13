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
import models.businessdetails.{BusinessDetails, PreviouslyRegisteredNo, PreviouslyRegisteredYes}
import models.businessmatching.BusinessMatching
import models.payments._
import models.renewal.Renewal
import models.registrationdetails.RegistrationDetails
import models.status._
import models.{status => _, _}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.HttpResponse
import services.cache.Cache
import utils.AmlsSpec
import views.html.confirmation.ConfirmationBacsView

import java.time.LocalDateTime
import scala.concurrent.Future

// scalastyle:off magic.number
class BacsConfirmationControllerSpec
    extends AmlsSpec
    with AmlsReferenceNumberGenerator
    with PaymentGenerator
    with SubscriptionResponseGenerator {

  trait Fixture {
    self =>
    val baseUrl                                  = "http://localhost"
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest(uri = baseUrl))
    lazy val view                                = app.injector.instanceOf[ConfirmationBacsView]
    val controller                               = new BacsConfirmationController(
      authAction = SuccessfulAuthAction,
      statusService = mock[StatusService],
      dataCacheConnector = mock[DataCacheConnector],
      amlsConnector = mock[AmlsConnector],
      enrolmentService = mock[AuthEnrolmentsService],
      ds = commonDependencies,
      cc = mockMcc,
      view = view
    )

    val amlsRegistrationNumber = "amlsRefNumber"

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

    when(controller.enrolmentService.amlsRegistrationNumber(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(amlsRegistrationNumber)))

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
      createdAt = LocalDateTime.now
    )

    val businessDetails = BusinessDetails(previouslyRegistered = Some(PreviouslyRegisteredNo))

    when {
      controller.dataCacheConnector.fetch[BusinessDetails](any(), eqTo(BusinessDetails.key))(any())
    } thenReturn Future.successful(Some(businessDetails))

    when {
      controller.dataCacheConnector.fetch[Renewal](any(), eqTo(Renewal.key))(any())
    } thenReturn Future.successful(None)

    val applicationConfig = app.injector.instanceOf[ApplicationConfig]

    def paymentsReturnLocation(ref: String) = ReturnLocation(
      controllers.routes.PaymentConfirmationController.paymentConfirmation(ref)
    )(applicationConfig)

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

  "BacsConfirmationController" must {

    "show the correct payment confirmation page" when {

      "bacs confirmation is requested" in new Fixture {

        when {
          controller.statusService.getReadStatus(any[String](), any[(String, String)]())(any(), any())
        } thenReturn Future.successful(ReadStatusResponse(LocalDateTime.now(), "", None, None, None, None, false))
        val result = controller.bacsConfirmation()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).getElementsByTag("h1").first().text() must include(
          messages("confirmation.payment.bacs.header")
        )
      }

      "bacs confirmation is requested and is a transitional renewal" in new Fixture {

        val businessDetailsYes = BusinessDetails(previouslyRegistered = Some(PreviouslyRegisteredYes(Some("123456"))))

        when {
          controller.statusService.getReadStatus(any[String](), any[(String, String)]())(any(), any())
        } thenReturn Future.successful(ReadStatusResponse(LocalDateTime.now(), "", None, None, None, None, false))

        when {
          controller.dataCacheConnector.fetch[BusinessDetails](any(), eqTo(BusinessDetails.key))(any())
        } thenReturn Future.successful(Some(businessDetailsYes))

        val result = controller.bacsConfirmation()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.html() must include(messages("confirmation.payment.renewal.info.hmrc_review"))
        doc.html() must include(messages("confirmation.payment.renewal.info.hmrc_review3"))
        doc.html() must include(messages("confirmation.payment.renewal.info.hmrc_review4"))

      }
    }
  }
}
