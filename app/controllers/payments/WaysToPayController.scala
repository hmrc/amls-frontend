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

package controllers.payments

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.FeeResponse
import models.payments.WaysToPay._
import models.payments.{CreateBacsPaymentRequest, WaysToPay}
import play.api.mvc.Result
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

@Singleton
class WaysToPayController @Inject()(
                                     val authConnector: AuthConnector,
                                     val statusService: StatusService,
                                     val paymentsService: PaymentsService,
                                     val authEnrolmentsService: AuthEnrolmentsService,
                                     val feeResponseService: FeeResponseService
                                   ) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        Future.successful(Ok(views.html.payments.ways_to_pay(EmptyForm)))
  }

  def post() = Authorised.async {
    implicit authContext => implicit request =>
        Form2[WaysToPay](request.body) match {
          case ValidForm(_, data) =>
            data match {
              case Card =>
                progressToPayment{ (fees, paymentReference, safeId) =>
                  paymentsService.requestPaymentsUrl(
                    fees,
                    controllers.routes.ConfirmationController.paymentConfirmation(paymentReference).url,
                    fees.amlsReferenceNumber,
                    safeId
                  ) map { paymentsRedirect =>
                    Redirect(paymentsRedirect.links.nextUrl)
                  }
                }("Cannot retrieve payment information")
              case Bacs =>
                progressToPayment{ (fees, paymentReference, safeId) =>
                  paymentsService.createBacsPayment(
                    CreateBacsPaymentRequest(
                      fees.amlsReferenceNumber,
                      paymentReference,
                      safeId,
                      paymentsService.amountFromSubmissionData(fees).fold(0)(_.map(_ * 100).value.toInt))
                  ) map { _ =>
                    Redirect(controllers.payments.routes.TypeOfBankController.get())
                  }
                }("Unable to save BACS info")
            }
          case f: InvalidForm => Future.successful(BadRequest(views.html.payments.ways_to_pay(f)))
        }
  }

  def progressToPayment(fn: (FeeResponse, String, String) => Future[Result])
                       (errorMessage: String)
                       (implicit ac: AuthContext, hc: HeaderCarrier): Future[Result] = {

    val submissionDetails = for {
      amlsRefNo <- OptionT(authEnrolmentsService.amlsRegistrationNumber)
      (_, detailedStatus) <- OptionT.liftF(statusService.getDetailedStatus(amlsRefNo))
      fees <- OptionT(feeResponseService.getFeeResponse(amlsRefNo))
    } yield (fees, detailedStatus)

    submissionDetails.value flatMap {
      case Some((fees, Some(detailedStatus)))
        if fees.paymentReference.isDefined & detailedStatus.safeId.isDefined =>
        fn(fees, fees.paymentReference.get, detailedStatus.safeId.get)
      case _ => Future.successful(InternalServerError(errorMessage))
    }
  }
}