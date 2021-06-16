/*
 * Copyright 2021 HM Revenue & Customs
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

import controllers.actions.SuccessfulAuthAction
import generators.{AmlsReferenceNumberGenerator, PaymentGenerator}
import models.ResponseType.SubscriptionResponseType
import models.confirmation.Currency
import models.payments._
import models.renewal.{AMLSTurnover, AMPTurnover, BusinessTurnover, CETransactionsInLast12Months, CashPayments, CashPaymentsCustomerNotMet, CustomersOutsideIsUK, CustomersOutsideUK, HowCashPaymentsReceived, InvolvedInOtherYes, MoneySources, MostTransactions, PaymentMethods, PercentageOfCashPaymentOver15000, Renewal, SendTheLargestAmountsOfMoney, TotalThroughput, TransactionsInLast12Months, WhichCurrencies}
import models.status.{SubmissionReady, SubmissionReadyForReview}
import models.{Country, FeeResponse, ReadStatusResponse, ReturnLocation}
import org.joda.time.DateTime
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AmlsSpec
import views.html.payments.ways_to_pay

import scala.concurrent.{ExecutionContext, Future}

class WaysToPayControllerSpec extends AmlsSpec with AmlsReferenceNumberGenerator with PaymentGenerator {

  trait Fixture {
    self =>

    val request = addToken(authRequest)
    val safeId = safeIdGen.sample.get

    implicit val hc: HeaderCarrier = new HeaderCarrier()
    implicit val ec: ExecutionContext = mock[ExecutionContext]
    lazy val view = app.injector.instanceOf[ways_to_pay]
    val controller = new WaysToPayController(
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      statusService = mock[StatusService],
      paymentsService = mock[PaymentsService],
      authEnrolmentsService = mock[AuthEnrolmentsService],
      feeResponseService = mock[FeeResponseService],
      cc = mockMcc,
      renewalService = mock[RenewalService],
      ways_to_pay = view
    )

    def paymentsReturnLocation(ref: String) = ReturnLocation(controllers.routes.PaymentConfirmationController.paymentConfirmation(ref))

    val fees = FeeResponse(SubscriptionResponseType, amlsRegistrationNumber, 100, None, None, 0, 100, Some(paymentReferenceNumber), None, DateTime.now())



    val submissionStatus = SubmissionReadyForReview
    val readStatusResponse = mock[ReadStatusResponse]

    when(readStatusResponse.safeId) thenReturn Some(safeId)

    when {
      controller.authEnrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())
    } thenReturn Future.successful(Some(amlsRegistrationNumber))

    when {
      controller.statusService.getStatus(any(), any())(any(), any())
    } thenReturn Future.successful(submissionStatus)

    when {
      controller.paymentsService.amountFromSubmissionData(any())
    } thenReturn Some(Currency.fromInt(100))

    when {
      controller.paymentsService.createBacsPayment(any(), any())(any(), any())
    } thenReturn Future.successful(paymentGen.sample.get)

    when {
      controller.paymentsService.updateBacsStatus(any(), any(), any())(any(), any())
    } thenReturn Future.successful(HttpResponse(OK))

    when {
      controller.statusService.getDetailedStatus(any[Option[String]], any(), any())( any(), any())
    } thenReturn Future.successful((submissionStatus, Some(readStatusResponse)))

    when {
      controller.feeResponseService.getFeeResponse(any(), any())(any(),any())
    } thenReturn Future.successful(Some(fees))

    when {
      controller.statusService.getStatus(any(), any(), any())(any(), any())
    } thenReturn Future.successful(SubmissionReady)

    val completeRenewal = Renewal(
      Some(InvolvedInOtherYes("test")),
      Some(BusinessTurnover.First),
      Some(AMLSTurnover.First),
      Some(AMPTurnover.First),
      Some(CustomersOutsideIsUK(true)),
      Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
      Some(PercentageOfCashPaymentOver15000.First),
      Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other")))))),
      Some(TotalThroughput("01")),
      Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
      Some(TransactionsInLast12Months("1500")),
      Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
      Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      Some(CETransactionsInLast12Months("123")),
      hasChanged = true
    )

    when(controller.renewalService.getRenewal(any())(any(), any()))
      .thenReturn(Future.successful(Some(completeRenewal)))

    when(controller.renewalService.isRenewalComplete(any(), any())(any(), any()))
      .thenReturn(Future.successful(true))

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
          val postRequest = requestWithUrlEncodedBody(
            "waysToPay" -> WaysToPay.Bacs.entryName
          )

          val result = controller.post()(postRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.payments.routes.TypeOfBankController.get().url))

          val bacsModel: CreateBacsPaymentRequest = CreateBacsPaymentRequest(amlsRegistrationNumber, paymentReferenceNumber, safeId, 10000)
          verify(controller.paymentsService).createBacsPayment(eqTo(bacsModel), any())(any(), any())

        }
      }

      "card" must {
        "go to the payments url" in new Fixture {
          val postRequest = requestWithUrlEncodedBody(
            "waysToPay" -> WaysToPay.Card.entryName
          )

          when {
            controller.paymentsService.requestPaymentsUrl(any(), any(), any(), any(), any())(any(), any(), any())
          } thenReturn Future.successful(NextUrl("/payments-next-url"))

          val result = controller.post()(postRequest)
          val body = contentAsString(result)

          verify(controller.paymentsService).requestPaymentsUrl(
            eqTo(fees),
            eqTo(paymentsReturnLocation(paymentReferenceNumber).url),
            eqTo(amlsRegistrationNumber),
            eqTo(safeId),
            any()
          )(any(), any(), any())

          redirectLocation(result) mustBe Some("/payments-next-url")
        }

        "return 500" when {
          "payment info cannot be retrieved" in new Fixture {

            val postRequest = requestWithUrlEncodedBody(
              "waysToPay" -> WaysToPay.Card.entryName
            )

            when {
              controller.authEnrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())
            } thenReturn Future.successful(None)

            when {
              controller.statusService.getStatus(any(), any())(any(), any())
            } thenReturn Future.successful(submissionStatus)

            val result = controller.post()(postRequest)

            status(result) mustBe 500
          }
        }
      }

      "request is invalid" must {
        "return BAD_REQUEST" in new Fixture {

          val postRequest = requestWithUrlEncodedBody(
            "waysToPay" -> "01"
          )

          val result = controller.post()(postRequest)
          status(result) mustBe BAD_REQUEST

        }
      }
    }

  }

}