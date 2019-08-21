/*
 * Copyright 2019 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.{AmlsConnector, PayApiConnector}
import javax.inject.Inject
import models.confirmation.Currency
import models.payments._
import models.{FeeResponse, ReturnLocation}
import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class PaymentsService @Inject()(val amlsConnector: AmlsConnector,
                                val paymentsConnector: PayApiConnector,
                                val statusService: StatusService) {

  def requestPaymentsUrl(fees: FeeResponse, returnUrl: String, amlsRefNo: String, safeId: String, accountTypeId: (String, String))
                        (implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Future[NextUrl] =
    fees match {
      case f: FeeResponse if f.difference.isDefined & f.paymentReference.isDefined =>
        paymentsUrlOrDefault(f.paymentReference.get, f.difference.get.toDouble, returnUrl, amlsRefNo, safeId, accountTypeId)
      case f: FeeResponse if f.paymentReference.isDefined =>
        paymentsUrlOrDefault(f.paymentReference.get, f.totalFees.toDouble, returnUrl, amlsRefNo, safeId, accountTypeId)
      case _ => Future.successful(NextUrl(ApplicationConfig.paymentsUrl))
    }

  def paymentsUrlOrDefault(paymentReference: String, amount: Double, returnUrl: String, amlsRefNo: String, safeId: String, accountTypeId: (String, String))
                          (implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Future[NextUrl] = {

    val amountInPence = (amount * 100).toInt

    paymentsConnector.createPayment(CreatePaymentRequest("other", paymentReference, "AMLS Payment", amountInPence, ReturnLocation(returnUrl))) flatMap {
      case Some(response) =>
        savePaymentBeforeResponse(response, amlsRefNo, safeId, accountTypeId).map(_ => response.nextUrl)
      case _ =>
        // $COVERAGE-OFF$
        Logger.warn("[ConfirmationController.requestPaymentUrl] Did not get a redirect url from the payments service; using configured default")
        // $COVERAGE-ON$
        Future.successful(NextUrl(ApplicationConfig.paymentsUrl))
    }
  }

  def updateBacsStatus(paymentReference: String, request: UpdateBacsRequest)
                      (implicit ec: ExecutionContext, hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] =
    amlsConnector.updateBacsStatus(paymentReference, request)

  def createBacsPayment(request: CreateBacsPaymentRequest, accountTypeId: (String, String))
                       (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Payment] =
    amlsConnector.createBacsPayment(request, accountTypeId)

  def amountFromSubmissionData(fees: FeeResponse): Option[Currency] = if(fees.difference.isDefined){
    fees.difference
  } else {
    Some(fees.totalFees)
  }

  private def savePaymentBeforeResponse(response: CreatePaymentResponse, amlsRefNo: String, safeId: String)
                                       (implicit hc: HeaderCarrier, authContext: AuthContext): Future[Unit] = {
      amlsConnector
        .savePayment(response.journeyId, amlsRefNo, safeId)
        .recover {
          case e => throw new Exception(s"Payment details failed to save. [paymentId:${response.journeyId}]", e)
        }.map(_ =>())
  }

  private def savePaymentBeforeResponse(response: CreatePaymentResponse, amlsRefNo: String, safeId: String, accountTypeId: (String, String))
                                       (implicit hc: HeaderCarrier): Future[Unit] = {
      amlsConnector
        .savePayment(response.journeyId, amlsRefNo, safeId, accountTypeId)
        .recover {
          case e => throw new Exception(s"Payment details failed to save. [paymentId:${response.journeyId}]", e)
        }.map(_ =>())
  }
}
