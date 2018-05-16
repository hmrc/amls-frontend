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

package controllers.payments

import generators.PaymentGenerator
import models.FeeResponse
import models.ResponseType.SubscriptionResponseType
import models.confirmation.Currency
import org.joda.time.DateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.{ExecutionContext, Future}

class TypeOfBankControllerSpec extends PlaySpec with AmlsSpec with PaymentGenerator {

  trait Fixture extends AuthorisedFixture { self =>

    val request = addToken(authRequest)

    implicit val hc: HeaderCarrier = new HeaderCarrier()
    implicit val ac: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    val controller = new TypeOfBankController(
      authConnector = self.authConnector,
      auditConnector = mock[AuditConnector],
      authEnrolmentsService = mock[AuthEnrolmentsService],
      feeResponseService = mock[FeeResponseService],
      paymentsService = mock[PaymentsService]
    )

    val paymentRef = paymentRefGen.sample.get

    when {
      controller.authEnrolmentsService.amlsRegistrationNumber(any(),any(),any())
    } thenReturn Future.successful(Some(amlsRegistrationNumber))

    when {
      controller.feeResponseService.getFeeResponse(eqTo(amlsRegistrationNumber))(any(),any(),any())
    } thenReturn Future.successful(Some(FeeResponse(
      SubscriptionResponseType,
      amlsRegistrationNumber,
      100,
      None,
      0,
      100,
      Some(paymentReferenceNumber),
      None,
      DateTime.now()
    )))

    when {
      controller.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(mock[AuditResult])

    when {
      controller.paymentsService.amountFromSubmissionData(any())
    } thenReturn Some(Currency.fromInt(100))

  }

  "TypeOfBankController" when {

    "get is called" must {
      "return OK with view" in new Fixture {

        val result = controller.get()(request)

        status(result) must be(OK)
        contentAsString(result) must include(Messages("payments.typeofbank.title"))

      }
    }

    "post is called" when {

      "form value is true" must {
        "redirect to BankDetails" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "typeOfBank" -> "true"
          )

          val result = controller.post()(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be (Some(controllers.payments.routes.BankDetailsController.get(true).url))
          verify(controller.auditConnector).sendEvent(any())(any(), any())
        }
      }

      "form value is false" must {
        "redirect to BankDetails" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "typeOfBank" -> "false"
          )

          val result = controller.post()(postRequest)
          val body = contentAsString(result)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) must be (Some(controllers.payments.routes.BankDetailsController.get(false).url))
          verify(controller.auditConnector).sendEvent(any())(any(), any())
        }
      }

      "request is invalid" must {
        "return BAD_REQUEST" in new Fixture {

          val postRequest = request.withFormUrlEncodedBody(
            "typeOfBank" -> "01"
          )

          val result = controller.post()(postRequest)
          status(result) mustBe BAD_REQUEST

        }
      }
    }

  }

}
