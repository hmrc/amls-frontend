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

package controllers.payments

import connectors.PayApiConnector
import generators.{AmlsReferenceNumberGenerator, PaymentGenerator}
import models.confirmation.Currency
import models.status.SubmissionReadyForReview
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, PaymentsService, StatusService, SubmissionResponseService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AmlsRefNumberBroker, AuthorisedFixture, GenericTestHelper}
import models.ReadStatusResponse
import models.payments.{WaysToPay, UpdateBacsRequest, CreatePaymentResponse, PayApiLinks, CreateBacsPaymentRequest}
import models.ReturnLocation
import cats.data.OptionT
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class WaysToPayControllerSpec extends PlaySpec with MockitoSugar with GenericTestHelper with AmlsReferenceNumberGenerator with PaymentGenerator {

  trait Fixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val safeId = safeIdGen.sample.get

    implicit val hc: HeaderCarrier = new HeaderCarrier()
    implicit val ac: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    val controller = new WaysToPayController(
      authConnector = self.authConnector,
      paymentsConnector = mock[PayApiConnector],
      statusService = mock[StatusService],
      paymentsService = mock[PaymentsService],
      submissionResponseService = mock[SubmissionResponseService],
      authEnrolmentsService = mock[AuthEnrolmentsService],
      amlsRefBroker = mock[AmlsRefNumberBroker]
    )

    def paymentsReturnLocation(ref: String) = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(ref))

    when {
      controller.paymentsService.updateBacsStatus(any(), any())(any(), any(), any())
    } thenReturn Future.successful(HttpResponse(OK))

    when {
      controller.amlsRefBroker.get(any(), any(), any())
    } thenReturn OptionT.pure[Future, String](amlsRegistrationNumber)

    val data = (Some(paymentReferenceNumber), Currency.fromInt(100), Seq(), Right(Some(Currency.fromInt(100))))

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
      controller.submissionResponseService.getSubmissionData(eqTo(submissionStatus))(any(), any(), any())
    } thenReturn Future.successful(Some(data))

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

      "enum is bacs" must {
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

      "enum is card" must {
        "go to the payments url" in new Fixture {
          val postRequest = request.withFormUrlEncodedBody(
            "waysToPay" -> WaysToPay.Card.entryName
          )

          when {
            controller.paymentsService.requestPaymentsUrl(any(), any(), any(), any())(any(), any(), any(), any())
          } thenReturn Future.successful(CreatePaymentResponse(PayApiLinks("/payments"), Some(amlsRegistrationNumber)))

          val result = controller.post()(postRequest)
          val body = contentAsString(result)

          verify(controller.paymentsService).requestPaymentsUrl(
            eqTo(data),
            eqTo(controllers.routes.ConfirmationController.paymentConfirmation(paymentReferenceNumber).url),
            eqTo(amlsRegistrationNumber),
            eqTo(safeId)
          )(any(), any(), any(), any())

          redirectLocation(result) mustBe Some("/payments")
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

            when {
              controller.submissionResponseService.getSubmissionData(eqTo(submissionStatus))(any(), any(), any())
            } thenReturn Future.successful(None)

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
