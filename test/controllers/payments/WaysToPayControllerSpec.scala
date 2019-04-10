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

package controllers.payments

import generators.{AmlsReferenceNumberGenerator, PaymentGenerator}
import models.ResponseType.SubscriptionResponseType
import models.confirmation.Currency
import models.payments._
import models.status.SubmissionReadyForReview
import models.{FeeResponse, ReadStatusResponse, ReturnLocation}
import org.joda.time.DateTime
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.{ExecutionContext, Future}

class WaysToPayControllerSpec extends AmlsSpec with AmlsReferenceNumberGenerator with PaymentGenerator {

  trait Fixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val safeId = safeIdGen.sample.get

    implicit val hc: HeaderCarrier = new HeaderCarrier()
    implicit val ac: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    val controller = new WaysToPayController(
      authConnector = self.authConnector,
      statusService = mock[StatusService],
      paymentsService = mock[PaymentsService],
      authEnrolmentsService = mock[AuthEnrolmentsService],
      feeResponseService = mock[FeeResponseService]
    )

    def paymentsReturnLocation(ref: String) = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(ref))

    val fees = FeeResponse(SubscriptionResponseType, amlsRegistrationNumber, 100, None, None, 0, 100, Some(paymentReferenceNumber), None, DateTime.now())

    when {
      controller.paymentsService.updateBacsStatus(any(), any())(any(), any(), any())
    } thenReturn Future.successful(HttpResponse(OK))

    val submissionStatus = SubmissionReadyForReview

    val readStatusResponse = mock[ReadStatusResponse]
    when(readStatusResponse.safeId) thenReturn Some(safeId)

    when {
      controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
    } thenReturn Future.successful(Some(amlsRegistrationNumber))

    when {
      controller.statusService.getStatus(any(), any(), any())
    } thenReturn Future.successful(submissionStatus)

    when {
      controller.paymentsService.amountFromSubmissionData(any())
    } thenReturn Some(Currency.fromInt(100))

    when {
      controller.paymentsService.createBacsPayment(any())(any(), any(), any())
    } thenReturn Future.successful(paymentGen.sample.get)

    when {
      controller.statusService.getDetailedStatus(any())(any(), any(), any())
    } thenReturn Future.successful((submissionStatus, Some(readStatusResponse)))

    when {
      controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(),any(),any())
    } thenReturn Future.successful(Some(fees))

  }

  "WaysToPayController" when {

    "get is called" must {
      "return OK with view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)
        contentAsString(result) must include(Messages("payments.waystopay.title"))

      }
    }

    "post is called" when {

      "bacs" must {
        "redirect to TypeOfBankController" in new Fixture {
          val postRequest = request.withFormUrlEncodedBody(
            "waysToPay" -> WaysToPay.Bacs.entryName
          )

          val result = controller.post()(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.payments.routes.TypeOfBankController.get().url))

          val bacsModel: CreateBacsPaymentRequest = CreateBacsPaymentRequest(amlsRegistrationNumber, paymentReferenceNumber, safeId, 10000)
          verify(controller.paymentsService).createBacsPayment(eqTo(bacsModel))(any(), any(), any())

        }
      }

      "card" must {
        "go to the payments url" in new Fixture {
          val postRequest = request.withFormUrlEncodedBody(
            "waysToPay" -> WaysToPay.Card.entryName
          )

          when {
            controller.paymentsService.requestPaymentsUrl(any(), any(), any(), any())(any(), any(), any(), any())
          } thenReturn Future.successful(NextUrl("/payments-next-url"))

          val result = controller.post()(postRequest)
          val body = contentAsString(result)

          verify(controller.paymentsService).requestPaymentsUrl(
            eqTo(fees),
            eqTo(controllers.routes.ConfirmationController.paymentConfirmation(paymentReferenceNumber).url),
            eqTo(amlsRegistrationNumber),
            eqTo(safeId)
          )(any(), any(), any(), any())

          redirectLocation(result) mustBe Some("/payments-next-url")
        }

        "return 500" when {
          "payment info cannot be retrieved" in new Fixture {

            val postRequest = request.withFormUrlEncodedBody(
              "waysToPay" -> WaysToPay.Card.entryName
            )

            when {
              controller.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
            } thenReturn Future.successful(None)

            when {
              controller.statusService.getStatus(any(), any(), any())
            } thenReturn Future.successful(submissionStatus)

            val result = controller.post()(postRequest)
            val body = contentAsString(result)

            status(result) mustBe 500
          }
        }
      }

      "request is invalid" must {
        "return BAD_REQUEST" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "waysToPay" -> "01"
          )

          val result = controller.post()(postRequest)
          status(result) mustBe BAD_REQUEST

        }
      }
    }

  }

}