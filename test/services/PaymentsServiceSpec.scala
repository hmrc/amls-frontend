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

package services

import cats.implicits._
import connectors.{AmlsConnector, PayApiConnector}
import generators.PaymentGenerator
import models.FeeResponse
import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import models.confirmation.Currency
import models.payments._
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import utils.AmlsSpec

import java.time.LocalDateTime
import scala.concurrent.Future

class PaymentsServiceSpec extends AmlsSpec with ScalaFutures with PaymentGenerator {

  // noinspection ScalaStyle
  trait Fixture {
    self =>

    val mockAmlsConnector  = mock[AmlsConnector]
    val testPaymentService = new PaymentsService(
      mockAmlsConnector,
      mock[PayApiConnector],
      mock[StatusService],
      appConfig
    )

    val paymentRefNo = "XA000000000000"
    val safeId       = amlsRefNoGen.sample.get

    val accountTypeId = ("accountType", "accountId")

    val currency = Currency.fromInt(100)

    val data = (paymentRefNo, currency, Seq(), Some(currency))

    val testFeeResponseSubscription = FeeResponse(
      responseType = SubscriptionResponseType,
      amlsReferenceNumber = "XAML0000000001",
      registrationFee = BigDecimal(100),
      fpFee = Some(BigDecimal(100)),
      approvalCheckFee = None,
      premiseFee = BigDecimal(100),
      totalFees = BigDecimal(100),
      paymentReference = Some("paymentReference"),
      difference = None,
      createdAt = LocalDateTime.of(2018, 1, 1, 0, 0)
    )

    val testFeeResponseAmendVariation = FeeResponse(
      responseType = AmendOrVariationResponseType,
      amlsReferenceNumber = "XAML0000000001",
      registrationFee = BigDecimal(100),
      fpFee = Some(BigDecimal(100)),
      approvalCheckFee = None,
      premiseFee = BigDecimal(100),
      totalFees = BigDecimal(100),
      paymentReference = Some("paymentReference"),
      difference = Some(BigDecimal(100)),
      createdAt = LocalDateTime.of(2018, 1, 1, 0, 0)
    )

    val paymentResponse = CreatePaymentResponse(nextUrl = NextUrl("http://return.com"), journeyId = "1234567890")

  }

  "PaymentService" when {

    "updateBacsStatus is called" must {
      "use the connector to update the bacs status" in new Fixture {
        val paymentRef = paymentRefGen.sample.get
        val request    = UpdateBacsRequest(true)

        when {
          testPaymentService.amlsConnector.updateBacsStatus(any(), any(), any())(any(), any())
        } thenReturn Future.successful(HttpResponse(OK, ""))

        whenReady(testPaymentService.updateBacsStatus(accountTypeId, paymentRef, request)) { _ =>
          verify(testPaymentService.amlsConnector)
            .updateBacsStatus(any(), eqTo(paymentRef), eqTo(request))(any(), any())
        }

      }
    }

    "createBacs payment is called" must {
      "use the connector to create a new bacs payment" in new Fixture {
        val request: CreateBacsPaymentRequest = createBacsPaymentGen.sample.get
        val payment: Payment                  = paymentGen.sample.get

        when {
          testPaymentService.amlsConnector.createBacsPayment(any(), eqTo(request))(any(), any())
        } thenReturn Future.successful(payment)

        whenReady(testPaymentService.createBacsPayment(request, accountTypeId)) {
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

          // noinspection ScalaStyle
          whenReady(
            testPaymentService.paymentsUrlOrDefault("ref", 100, "http://return.com", "ref-no", "safeid", accountTypeId)
          ) { result =>
            result mustBe NextUrl(appConfig.paymentsUrl)
          }

        }
      }
    }
  }

  "requestPaymentsUrl" when {
    "called" must {
      "return payments url" when {
        "difference and payment reference are defined in FeeResponse" in new Fixture {
          when {
            testPaymentService.paymentsConnector.createPayment(any())(any(), any())
          } thenReturn Future.successful(Some(paymentResponse))

          when {
            mockAmlsConnector.savePayment(any(), any(), any(), any())(any(), any())
          } thenReturn Future.successful(HttpResponse(CREATED, ""))

          whenReady(
            testPaymentService.requestPaymentsUrl(
              testFeeResponseAmendVariation,
              "http://return.com",
              "XAML0000000001",
              safeId,
              accountTypeId
            )
          ) { result =>
            result mustBe NextUrl("http://return.com")
          }
        }

        "payment reference is defined in FeeResponse" in new Fixture {
          when {
            testPaymentService.paymentsConnector.createPayment(any())(any(), any())
          } thenReturn Future.successful(Some(paymentResponse))

          when {
            mockAmlsConnector.savePayment(any(), any(), any(), any())(any(), any())
          } thenReturn Future.successful(HttpResponse(CREATED, ""))

          whenReady(
            testPaymentService.requestPaymentsUrl(
              testFeeResponseSubscription,
              "http://return.com",
              "XAML0000000001",
              safeId,
              accountTypeId
            )
          ) { result =>
            result mustBe NextUrl("http://return.com")
          }
        }
      }

      "return default payments url" in new Fixture {

        whenReady(
          testPaymentService.requestPaymentsUrl(
            testFeeResponseAmendVariation.copy(difference = None, paymentReference = None),
            "http://return.com",
            "XAML0000000001",
            safeId,
            accountTypeId
          )
        ) { result =>
          result mustBe NextUrl(appConfig.paymentsUrl)
        }
      }
    }
  }

  "amountFromSubmissionData" when {
    "called with fee response" must {
      "return difference if it exists" in new Fixture {
        val result = testPaymentService.amountFromSubmissionData(testFeeResponseAmendVariation)

        result mustBe Currency(100).some
      }

      "return total fees if difference does not exists" in new Fixture {
        val result = testPaymentService.amountFromSubmissionData(testFeeResponseSubscription)

        result mustBe Currency(100).some
      }
    }
  }

  "savePaymentBeforeResponse" when {
    "called" must {
      "fail if cannot save payment" in new Fixture with PrivateMethodTester {
        when {
          mockAmlsConnector.savePayment(any(), any(), any(), any())(any(), any())
        } thenThrow new IllegalArgumentException()

        val savePaymentBeforeResponse = PrivateMethod[Future[Unit]](Symbol("savePaymentBeforeResponse"))

        intercept[IllegalArgumentException] {
          testPaymentService invokePrivate savePaymentBeforeResponse(paymentResponse, "xxx", "zzz", headerCarrier)
        }
      }
    }
  }
}
