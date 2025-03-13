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

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import generators.PaymentGenerator
import models.ResponseType.SubscriptionResponseType
import models.renewal.{AMLSTurnover, AMPTurnover, BusinessTurnover, CETransactionsInLast12Months, CashPayments, CashPaymentsCustomerNotMet, CustomersOutsideIsUK, CustomersOutsideUK, HowCashPaymentsReceived, InvolvedInOtherYes, MoneySources, MostTransactions, PaymentMethods, PercentageOfCashPaymentOver15000, Renewal, SendTheLargestAmountsOfMoney, TotalThroughput, TransactionsInLast12Months, WhichCurrencies}
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview}
import models.{Country, FeeResponse, SubmissionRequestStatus}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, FeeResponseService, RenewalService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AmlsSpec, DependencyMocks}
import views.html.payments.BankDetailsView

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class BankDetailsControllerSpec extends AmlsSpec with PaymentGenerator {

  trait Fixture extends DependencyMocks { self =>

    val request = addToken(authRequest)

    implicit val hc: HeaderCarrier    = new HeaderCarrier()
    implicit val ec: ExecutionContext = mock[ExecutionContext]
    lazy val view                     = app.injector.instanceOf[BankDetailsView]
    val controller                    = new BankDetailsController(
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      authEnrolmentsService = mock[AuthEnrolmentsService],
      feeResponseService = mock[FeeResponseService],
      statusService = mockStatusService,
      cc = mockMcc,
      renewalService = mock[RenewalService],
      view = view
    )

    val submissionStatus = SubmissionReadyForReview

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

  "BankDetailsController" when {

    "get is called" must {
      "return OK with view" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)

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
              200,
              Some(paymentReferenceNumber),
              None,
              LocalDateTime.now()
            )
          )
        )

        when {
          controller.dataCacheConnector.fetch[SubmissionRequestStatus](any(), eqTo(SubmissionRequestStatus.key))(any())
        } thenReturn Future.successful(Some(SubmissionRequestStatus(true)))

        val result = controller.get()(request)

        status(result)          must be(OK)
        contentAsString(result) must include(Messages("payments.bankdetails.title"))

      }
    }
  }
}
