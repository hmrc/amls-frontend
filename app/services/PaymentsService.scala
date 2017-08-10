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
                               val submissionResponseService: SubmissionResponseService
                               ){

  type ViewData = (String, Currency, Seq[BreakdownRow], Option[Currency])

  def requestPaymentsUrl(data: ViewData, returnUrl: String, amlsRefNo: String)
                                (implicit hc: HeaderCarrier,
                                 ec: ExecutionContext,
                                 authContext: AuthContext,
                                 request: Request[_]): Future[CreatePaymentResponse] =
    data match {
      case (ref, _, _, Some(difference)) => paymentsUrlOrDefault(ref, difference, returnUrl, amlsRefNo)
      case (ref, total, _, None) => paymentsUrlOrDefault(ref, total, returnUrl, amlsRefNo)
      case _ => Future.successful(CreatePaymentResponse.default)
    }

  def paymentsUrlOrDefault(ref: String, amount: Double, returnUrl: String, amlsRefNo: String)
                                  (implicit hc: HeaderCarrier,
                                   ec: ExecutionContext,
                                   authContext: AuthContext,
                                   request: Request[_]): Future[CreatePaymentResponse] = {


    val amountInPence = (amount * 100).toInt

    paymentsConnector.createPayment(CreatePaymentRequest("other", ref, "AMLS Payment", amountInPence, ReturnLocation(returnUrl))) flatMap {
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

  def getAmendmentFees(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[ViewData]] = {
    submissionResponseService.getAmendment flatMap {
      case Some((paymentRef, total, rows, difference)) =>
        Future.successful(
          (difference, paymentRef) match {
            case (Some(currency), Some(payRef)) if currency.value > 0 => Some((payRef, total, rows, difference))
            case _ => None
          }
        )
      case None => Future.failed(new Exception("Cannot get data from amendment submission"))
    }
  }

}