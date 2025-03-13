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

package controllers.payments

import controllers.actions.SuccessfulAuthAction
import forms.payments.TypeOfBankFormProvider
import generators.PaymentGenerator
import models.{Country, FeeResponse}
import models.ResponseType.SubscriptionResponseType
import models.confirmation.Currency
import models.renewal.{AMLSTurnover, AMPTurnover, BusinessTurnover, CETransactionsInLast12Months, CashPayments, CashPaymentsCustomerNotMet, CustomersOutsideIsUK, CustomersOutsideUK, HowCashPaymentsReceived, InvolvedInOtherYes, MoneySources, MostTransactions, PaymentMethods, PercentageOfCashPaymentOver15000, Renewal, SendTheLargestAmountsOfMoney, TotalThroughput, TransactionsInLast12Months, WhichCurrencies}
import models.status.SubmissionReady
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.AmlsSpec
import views.html.payments.TypeOfBankView

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class TypeOfBankControllerSpec extends PlaySpec with AmlsSpec with PaymentGenerator with Injecting {

  trait Fixture { self =>

    val request = addToken(authRequest)

    implicit val hc: HeaderCarrier    = new HeaderCarrier()
    implicit val ec: ExecutionContext = mock[ExecutionContext]
    lazy val view                     = inject[TypeOfBankView]
    val controller                    = new TypeOfBankController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      auditConnector = mock[AuditConnector],
      authEnrolmentsService = mock[AuthEnrolmentsService],
      feeResponseService = mock[FeeResponseService],
      paymentsService = mock[PaymentsService],
      cc = mockMcc,
      statusService = mock[StatusService],
      renewalService = mock[RenewalService],
      formProvider = inject[TypeOfBankFormProvider],
      view = view
    )

    val paymentRef = paymentRefGen.sample.get

    when {
      controller.authEnrolmentsService.amlsRegistrationNumber(any(), any())(any(), any())
    } thenReturn Future.successful(Some(amlsRegistrationNumber))

    when {
      controller.feeResponseService.getFeeResponse(any(), any())(any(), any())
    } thenReturn Future.successful(
      Some(
        FeeResponse(
          SubscriptionResponseType,
          amlsRegistrationNumber,
          100,
          None,
          None,
          0,
          100,
          Some(paymentReferenceNumber),
          None,
          LocalDateTime.now()
        )
      )
    )

    when {
      controller.auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(mock[AuditResult])

    when {
      controller.paymentsService.amountFromSubmissionData(any())
    } thenReturn Some(Currency.fromInt(100))

    when {
      controller.statusService.getStatus(any(), any(), any())(any(), any(), any())
    } thenReturn Future.successful(SubmissionReady)

    val completeRenewal = Renewal(
      Some(InvolvedInOtherYes("test")),
      Some(BusinessTurnover.First),
      Some(AMLSTurnover.First),
      Some(AMPTurnover.First),
      Some(CustomersOutsideIsUK(true)),
      Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
      Some(PercentageOfCashPaymentOver15000.First),
      Some(
        CashPayments(
          CashPaymentsCustomerNotMet(true),
          Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other"))))
        )
      ),
      Some(TotalThroughput("01")),
      Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
      Some(TransactionsInLast12Months("1500")),
      Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
      Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      Some(CETransactionsInLast12Months("123")),
      hasChanged = true
    )

    when(controller.renewalService.getRenewal(any())).thenReturn(Future.successful(Some(completeRenewal)))

    when(controller.renewalService.isRenewalComplete(any(), any())(any()))
      .thenReturn(Future.successful(true))
  }

  "TypeOfBankController" when {

    "get is called" must {
      "return OK with view" in new Fixture {

        val result = controller.get()(request)

        status(result)          must be(OK)
        contentAsString(result) must include(messages("payments.typeofbank.title"))

      }
    }

    "post is called" when {

      "form value is true" must {
        "redirect to BankDetails" in new Fixture {

          val postRequest = FakeRequest(POST, routes.TypeOfBankController.post().url).withFormUrlEncodedBody(
            "typeOfBank" -> "true"
          )

          val result = controller.post()(postRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.payments.routes.BankDetailsController.get(true).url))
          verify(controller.auditConnector).sendEvent(any())(any(), any())
        }
      }

      "form value is false" must {
        "redirect to BankDetails" in new Fixture {

          val postRequest = FakeRequest(POST, routes.TypeOfBankController.post().url).withFormUrlEncodedBody(
            "typeOfBank" -> "false"
          )

          val result = controller.post()(postRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) must be(Some(controllers.payments.routes.BankDetailsController.get(false).url))
          verify(controller.auditConnector).sendEvent(any())(any(), any())
        }
      }

      "request is invalid" must {
        "return BAD_REQUEST" in new Fixture {

          val postRequest = FakeRequest(POST, routes.TypeOfBankController.post().url).withFormUrlEncodedBody(
            "typeOfBank" -> "01"
          )

          val result = controller.post()(postRequest)
          status(result) mustBe BAD_REQUEST

        }
      }
    }

  }

}
