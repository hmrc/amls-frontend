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

package controllers.payments

import cats.data.OptionT
import cats.implicits._
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.FeeResponse
import models.payments.WaysToPay._
import models.payments.{CreateBacsPaymentRequest, WaysToPay}
import play.api.mvc.Result
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, AuthorisedRequest}

import scala.concurrent.Future

@Singleton
class WaysToPayController @Inject()(
                                     val authAction: AuthAction, val ds: CommonPlayDependencies,
                                     val statusService: StatusService,
                                     val paymentsService: PaymentsService,
                                     val authEnrolmentsService: AuthEnrolmentsService,
                                     val feeResponseService: FeeResponseService
                                   ) extends AmlsBaseController(ds) {

  def get() = authAction.async {
      implicit request =>
        Future.successful(Ok(views.html.payments.ways_to_pay(EmptyForm)))
  }

  def post() = authAction.async {
    implicit request =>
        Form2[WaysToPay](request.body) match {
          case ValidForm(_, data) =>
            data match {
              case Card =>
                progressToPayment{ (fees, paymentReference, safeId) =>
                  paymentsService.requestPaymentsUrl(
                    fees,
                    controllers.routes.PaymentConfirmationController.paymentConfirmation(paymentReference).url,
                    fees.amlsReferenceNumber,
                    safeId,
                    request.accountTypeId
                  ).map { nextUrl =>
                    Redirect(nextUrl.value)
                  }
                }("Cannot retrieve payment information")
              case Bacs =>
                progressToPayment{ (fees, paymentReference, safeId) =>
                  paymentsService.createBacsPayment(
                    CreateBacsPaymentRequest(
                      fees.amlsReferenceNumber,
                      paymentReference,
                      safeId,
                      paymentsService.amountFromSubmissionData(fees).fold(0)(_.map(_ * 100).value.toInt)),
                    request.accountTypeId
                  ).map { _ =>
                    Redirect(controllers.payments.routes.TypeOfBankController.get())
                  }
                }("Unable to save BACS info")
            }
          case f: InvalidForm => Future.successful(BadRequest(views.html.payments.ways_to_pay(f)))
        }
  }

  def progressToPayment(fn: (FeeResponse, String, String) => Future[Result])
                       (errorMessage: String)
                       (implicit hc: HeaderCarrier, request:AuthorisedRequest[_]): Future[Result] = {

    val submissionDetails = for {
      amlsRefNo <- OptionT(authEnrolmentsService.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier))
      (_, detailedStatus) <- OptionT.liftF(statusService.getDetailedStatus(Option(amlsRefNo), request.accountTypeId, request.credId))
      fees <- OptionT(feeResponseService.getFeeResponse(amlsRefNo, request.accountTypeId))
    } yield (fees, detailedStatus)

    submissionDetails.value flatMap {
      case Some((fees, Some(detailedStatus)))
        if fees.paymentReference.isDefined & detailedStatus.safeId.isDefined =>
        fn(fees, fees.paymentReference.get, detailedStatus.safeId.get)
      case _ => Future.successful(InternalServerError(errorMessage))
    }
  }
}