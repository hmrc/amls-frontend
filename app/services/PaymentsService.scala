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

package services

import javax.inject.Inject

import cats.implicits._
import cats.data.OptionT
import connectors.{AmlsConnector, PayApiConnector}
import models.confirmation.{BreakdownRow, Currency}
import models.payments.{CreatePaymentRequest, CreatePaymentResponse, ReturnLocation}
import models.status.SubmissionReadyForReview
import play.api.{Logger, Play}
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.http.Status._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class PaymentsService @Inject()(
                               val amlsConnector: AmlsConnector,
                               val paymentsConnector: PayApiConnector,
                               val submissionResponseService: SubmissionResponseService,
                               val statusService: StatusService
                               ){

  type SubmissionData = (Option[String], Currency, Seq[BreakdownRow], Either[String, Option[Currency]])

  def requestPaymentsUrl(data: SubmissionData, returnUrl: String, amlsRefNo: String)
                                (implicit hc: HeaderCarrier,
                                 ec: ExecutionContext,
                                 authContext: AuthContext,
                                 request: Request[_]): Future[CreatePaymentResponse] =
    data match {
      case (Some(ref), _, _, Right(Some(difference))) => paymentsUrlOrDefault(ref, difference, returnUrl, amlsRefNo)
      case (Some(ref), total, _, Right(None)) => paymentsUrlOrDefault(ref, total, returnUrl, amlsRefNo)
      case _ => Future.successful(CreatePaymentResponse.default)
    }

  def paymentsUrlOrDefault(paymentReference: String, amount: Double, returnUrl: String, amlsRefNo: String)
                                  (implicit hc: HeaderCarrier,
                                   ec: ExecutionContext,
                                   authContext: AuthContext,
                                   request: Request[_]): Future[CreatePaymentResponse] = {


    val amountInPence = (amount * 100).toInt

    paymentsConnector.createPayment(CreatePaymentRequest("other", paymentReference, "AMLS Payment", amountInPence, ReturnLocation(returnUrl))) flatMap {
      case Some(response) => savePaymentBeforeResponse(response, amlsRefNo)
      case _ =>
        Logger.warn("[ConfirmationController.requestPaymentUrl] Did not get a redirect url from the payments service; using configured default")
        Future.successful(CreatePaymentResponse.default)
    }

  }

  private def savePaymentBeforeResponse(response: CreatePaymentResponse, amlsRefNo: String)(implicit hc: HeaderCarrier, authContext: AuthContext) = {
    (for {
      paymentId <- OptionT.fromOption[Future](response.paymentId)
      payment <- OptionT.liftF(amlsConnector.savePayment(paymentId, amlsRefNo))
    } yield payment.status).value flatMap {
      case Some(CREATED) => Future.successful(response)
      case res => Future.failed(new Exception(s"Payment details failed to save. Response: $res"))
    }
  }

}
