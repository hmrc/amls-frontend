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

import connectors.{PayApiConnector, PaymentsConnector}
import generators.AmlsReferenceNumberGenerator
import models.confirmation.Currency
import models.payments._
import models.renewal.{InvolvedInOtherNo, Renewal}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.{verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{PaymentsService, StatusService, SubmissionResponseService}
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class WaysToPayControllerSpec extends PlaySpec with MockitoSugar with GenericTestHelper with AmlsReferenceNumberGenerator {

  trait Fixture extends AuthorisedFixture { self =>

    val request = addToken(authRequest)

    val controller = new WaysToPayController(
      authConnector = self.authConnector,
      paymentsConnector = mock[PayApiConnector],
      statusService = mock[StatusService],
      paymentsService = mock[PaymentsService],
      submissionResponseService = mock[SubmissionResponseService]
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
        "go to the payments url for an amendment" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "waysToPay" -> WaysToPay.Card.entryName
          )

          when {
            controller.statusService.getStatus(any(), any(), any())
          } thenReturn Future.successful(SubmissionReadyForReview)

          when {
            controller.submissionResponseService.getAmendment(any(), any(), any())
          } thenReturn Future.successful(Some((Some(paymentRefNo), Currency.fromInt(100), Seq(), Some(Currency.fromInt(100)))))

          when {
            controller.paymentsService.requestPaymentsUrl(
              (paymentRefNo, Currency.fromInt(100), Seq(), Some(Currency.fromInt(100))),
              "/paymentConfirmation",
              amlsRegistrationNumber)(any(),any(),any(),any())
          } thenReturn Future.successful(CreatePaymentResponse(PayApiLinks("/payments"), Some(amlsRegistrationNumber)))

          val result = controller.post()(postRequest)
          val body = contentAsString(result)

          //noinspection ScalaStyle
          verify(controller.paymentsConnector).createPayment(eqTo{
            CreatePaymentRequest("other", paymentRefNo, "AMLS Payment", 10000, paymentsReturnLocation(paymentRefNo))
          })(any(), any())

          verify(controller.paymentsService).requestPaymentsUrl(
            (paymentRefNo, Currency.fromInt(100), Seq(), Some(Currency.fromInt(100))),
            "/paymentConfirmation",
            amlsRegistrationNumber
          )(any(),any(),any(),any())

          redirectLocation(result) mustBe "/payments"
        }

      }
    }

  }

}
