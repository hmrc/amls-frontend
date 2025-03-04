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

import config.ApplicationConfig
import connectors.{AmlsConnector, PayApiConnector}
import models.confirmation.Currency
import models.payments._
import models.{FeeResponse, ReturnLocation}
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsService @Inject() (
  val amlsConnector: AmlsConnector,
  val paymentsConnector: PayApiConnector,
  val statusService: StatusService,
  val applicationConfig: ApplicationConfig
) extends Logging {

  def requestPaymentsUrl(
    fees: FeeResponse,
    returnUrl: String,
    amlsRefNo: String,
    safeId: String,
    accountTypeId: (String, String)
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NextUrl] =
    fees match {
      case f: FeeResponse if f.difference.isDefined & f.paymentReference.isDefined =>
        paymentsUrlOrDefault(
          f.paymentReference.get,
          f.difference.get.toDouble,
          returnUrl,
          amlsRefNo,
          safeId,
          accountTypeId
        )
      case f: FeeResponse if f.paymentReference.isDefined                          =>
        paymentsUrlOrDefault(f.paymentReference.get, f.totalFees.toDouble, returnUrl, amlsRefNo, safeId, accountTypeId)
      case _                                                                       => Future.successful(NextUrl(applicationConfig.paymentsUrl))
    }

  def paymentsUrlOrDefault(
    paymentReference: String,
    amount: Double,
    returnUrl: String,
    amlsRefNo: String,
    safeId: String,
    accountTypeId: (String, String)
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NextUrl] = {

    val amountInPence = (amount * 100).toInt

    paymentsConnector.createPayment(
      CreatePaymentRequest(
        "other",
        paymentReference,
        "AMLS Payment",
        amountInPence,
        ReturnLocation(returnUrl)(applicationConfig)
      )
    ) flatMap {
      case Some(response) =>
        savePaymentBeforeResponse(response, amlsRefNo, safeId, accountTypeId).map(_ => response.nextUrl)
      case _              =>
        // $COVERAGE-OFF$
        logger.warn(
          "[ConfirmationController.requestPaymentUrl] Did not get a redirect url from the payments service; using configured default"
        )
        // $COVERAGE-ON$
        Future.successful(NextUrl(applicationConfig.paymentsUrl))
    }
  }

  def updateBacsStatus(accountTypeId: (String, String), paymentReference: String, request: UpdateBacsRequest)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[HttpResponse] =
    amlsConnector.updateBacsStatus(accountTypeId, paymentReference, request)

  def createBacsPayment(request: CreateBacsPaymentRequest, accountTypeId: (String, String))(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Payment] =
    amlsConnector.createBacsPayment(accountTypeId, request)

  def amountFromSubmissionData(fees: FeeResponse): Option[Currency] = if (fees.difference.isDefined) {
    fees.difference
  } else {
    Some(fees.totalFees)
  }

  private def savePaymentBeforeResponse(
    response: CreatePaymentResponse,
    amlsRefNo: String,
    safeId: String,
    accountTypeId: (String, String)
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    amlsConnector
      .savePayment(response.journeyId, amlsRefNo, safeId, accountTypeId)
      .recover { case e =>
        throw new Exception(s"Payment details failed to save. [paymentId:${response.journeyId}]", e)
      }
      .map(_ => ())
}
