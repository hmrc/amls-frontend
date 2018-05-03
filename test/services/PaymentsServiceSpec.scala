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

package services

import connectors.{AmlsConnector, PayApiConnector}
import generators.PaymentGenerator
import models.confirmation.Currency
import models.payments.{CreateBacsPaymentRequest, CreatePaymentResponse, Payment, UpdateBacsRequest}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AuthorisedFixture, AmlsSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class PaymentsServiceSpec extends AmlsSpec with ScalaFutures with PaymentGenerator {

  //noinspection ScalaStyle
  trait Fixture extends AuthorisedFixture {
    self =>

    val testPaymentService = new PaymentsService(
      mock[AmlsConnector],
      mock[PayApiConnector],
      mock[SubmissionResponseService],
      mock[StatusService]
    )

    val paymentRefNo = "XA000000000000"
    val safeId = amlsRefNoGen.sample.get

    val currency = Currency.fromInt(100)

    val data = (paymentRefNo, currency, Seq(), Some(currency))

  }

  "PaymentService" when {

    "updateBacsStatus is called" must {
      "use the connector to update the bacs status" in new Fixture {
        val paymentRef = paymentRefGen.sample.get
        val request = UpdateBacsRequest(true)

        when {
          testPaymentService.amlsConnector.updateBacsStatus(any(), any())(any(), any(), any())
        } thenReturn Future.successful(HttpResponse(OK))

        whenReady(testPaymentService.updateBacsStatus(paymentRef, request)) { _ =>
          verify(testPaymentService.amlsConnector).updateBacsStatus(eqTo(paymentRef), eqTo(request))(any(), any(), any())
        }

      }
    }

    "createBacs payment is called" must {
      "use the connector to create a new bacs payment" in new Fixture {
        val request: CreateBacsPaymentRequest = createBacsPaymentGen.sample.get
        val payment: Payment = paymentGen.sample.get

        when {
          testPaymentService.amlsConnector.createBacsPayment(eqTo(request))(any(), any(), any())
        } thenReturn Future.successful(payment)

        whenReady(testPaymentService.createBacsPayment(request)) {
          _ mustBe payment
        }
      }
    }
  }

  "paymentsUrlOrDefault" when {
    "called" must {
      "return the default url" when {
        "no response was returned from the connector" in new Fixture {
          when {
            testPaymentService.paymentsConnector.createPayment(any())(any(), any())
          } thenReturn Future.successful(None)

          //noinspection ScalaStyle
          whenReady(testPaymentService.paymentsUrlOrDefault("ref", 100, "http://return.com", "ref-no", "safeid")) { result =>
            result mustBe CreatePaymentResponse.default
          }

        }
      }
    }
  }

}
