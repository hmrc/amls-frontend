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
import generators.AmlsReferenceNumberGenerator
import models.confirmation.Currency
import models.payments._
import models.status.SubmissionReadyForReview
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.{verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, PaymentsService, StatusService, SubmissionResponseService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.{ExecutionContext, Future}

class WaysToPayControllerSpec extends PlaySpec with MockitoSugar with GenericTestHelper with AmlsReferenceNumberGenerator {

  trait Fixture extends AuthorisedFixture { self =>

    val request = addToken(authRequest)

    implicit val hc: HeaderCarrier = new HeaderCarrier()
    implicit val ac: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    val controller = new WaysToPayController(
      authConnector = self.authConnector,
      paymentsConnector = mock[PayApiConnector],
      statusService = mock[StatusService],
      paymentsService = mock[PaymentsService],
      submissionResponseService = mock[SubmissionResponseService],
      authEnrolmentsService = mock[AuthEnrolmentsService]
    )

    val paymentRefNo = "XA000000000000"

    def paymentsReturnLocation(ref: String) = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation(ref))

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
          redirectLocation(result) must be (Some(controllers.payments.routes.TypeOfBankController.get().url))

        }
      }

      "enum is card" must {
        "go to the payments url" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "waysToPay" -> WaysToPay.Card.entryName
          )

          val data = (Some(paymentRefNo), Currency.fromInt(100), Seq(), Right(Some(Currency.fromInt(100))))

          val status = SubmissionReadyForReview

          when {
            controller.authEnrolmentsService.amlsRegistrationNumber(any(),any(),any())
          } thenReturn Future.successful(Some(amlsRegistrationNumber))

          when {
            controller.statusService.getStatus(any(),any(),any())
          } thenReturn Future.successful(status)

          when {
            controller.submissionResponseService.getSubmissionData(eqTo(status))(any(),any(),any())
          } thenReturn Future.successful(Some(data))

          when {
            controller.paymentsService.requestPaymentsUrl(any(),any(),any())(any(),any(),any(),any())
          } thenReturn Future.successful(CreatePaymentResponse(PayApiLinks("/payments"), Some(amlsRegistrationNumber)))

          val result = controller.post()(postRequest)
          val body = contentAsString(result)

          verify(controller.paymentsService).requestPaymentsUrl(
            eqTo(data),
            eqTo(controllers.routes.ConfirmationController.paymentConfirmation(paymentRefNo).url),
            eqTo(amlsRegistrationNumber)
          )(any(),any(),any(),any())

          redirectLocation(result) mustBe Some("/payments")
        }

        "go to the default payments url when submission data cannot be retrieved" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "waysToPay" -> WaysToPay.Card.entryName
          )

          val data = (paymentRefNo, Currency.fromInt(100), Seq(), Some(Currency.fromInt(100)))

          val status = SubmissionReadyForReview

          when {
            controller.statusService.getStatus(any(), any(), any())
          } thenReturn Future.successful(status)

          when {
            controller.authEnrolmentsService.amlsRegistrationNumber(any(),any(),any())
          } thenReturn Future.successful(Some(amlsRegistrationNumber))

          when {
            controller.submissionResponseService.getSubmissionData(eqTo(status))(any(),any(),any())
          } thenReturn Future.successful(None)

          val result = controller.post()(postRequest)
          val body = contentAsString(result)

          redirectLocation(result) mustBe Some(CreatePaymentResponse.default.links.nextUrl)
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
