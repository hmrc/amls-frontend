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

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, PayApiConnector}
import models.{FeeResponse, ReturnLocation}
import models.confirmation.{BreakdownRow, Currency}
import models.payments._
import play.api.Logger
import play.api.http.Status._
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}


class PaymentsService @Inject()(
                                 val amlsConnector: AmlsConnector,
                                 val paymentsConnector: PayApiConnector,
                                 val submissionResponseService: ConfirmationService,
                                 val statusService: StatusService
                               ) {

  def requestPaymentsUrl(fees: FeeResponse, returnUrl: String, amlsRefNo: String, safeId: String)
                        (implicit hc: HeaderCarrier,
                         ec: ExecutionContext,
                         authContext: AuthContext,
                         request: Request[_]): Future[CreatePaymentResponse] =
    fees match {
      case f:FeeResponse if f.difference.isDefined & f.paymentReference.isDefined =>
        paymentsUrlOrDefault(f.paymentReference.get, f.difference.get.toDouble, returnUrl, amlsRefNo, safeId)
      case f:FeeResponse if f.paymentReference.isDefined =>
        paymentsUrlOrDefault(f.paymentReference.get, f.totalFees.toDouble, returnUrl, amlsRefNo, safeId)
      case _ => Future.successful(CreatePaymentResponse.default)
    }

  //noinspection ScalaStyle
  def paymentsUrlOrDefault(paymentReference: String,
                           amount: Double,
                           returnUrl: String,
                           amlsRefNo: String,
                           safeId: String)
                          (implicit hc: HeaderCarrier,
                           ec: ExecutionContext,
                           authContext: AuthContext,
                           request: Request[_]): Future[CreatePaymentResponse] = {


    val amountInPence = (amount * 100).toInt

    paymentsConnector.createPayment(CreatePaymentRequest("other", paymentReference, "AMLS Payment", amountInPence, ReturnLocation(returnUrl))) flatMap {
      case Some(response) => savePaymentBeforeResponse(response, amlsRefNo, safeId)
      case _ =>
        // $COVERAGE-OFF$
        Logger.warn("[ConfirmationController.requestPaymentUrl] Did not get a redirect url from the payments service; using configured default")
        // $COVERAGE-ON$
        Future.successful(CreatePaymentResponse.default)
    }

  }

  def updateBacsStatus(paymentReference: String, request: UpdateBacsRequest)
                      (implicit ec: ExecutionContext, hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] =
    amlsConnector.updateBacsStatus(paymentReference, request)

  def createBacsPayment(request: CreateBacsPaymentRequest)
                       (implicit ec: ExecutionContext, hc: HeaderCarrier, ac: AuthContext): Future[Payment] =
    amlsConnector.createBacsPayment(request)

  def amountFromSubmissionData(fees: FeeResponse): Option[Currency] = if(fees.difference.isDefined){
    fees.difference
  } else {
    Some(fees.totalFees)
  }

  private def savePaymentBeforeResponse(response: CreatePaymentResponse, amlsRefNo: String, safeId: String)
                                       (implicit hc: HeaderCarrier, authContext: AuthContext) = {
    (for {
      paymentId <- OptionT.fromOption[Future](response.paymentId)
      payment <- OptionT.liftF(amlsConnector.savePayment(paymentId, amlsRefNo, safeId))
    } yield payment.status).value flatMap {
      case Some(CREATED) => Future.successful(response)
      case res => Future.failed(new Exception(s"Payment details failed to save. Response: $res"))
    }
  }

}
