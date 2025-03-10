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

import cats.data.OptionT
import cats.implicits._
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.payments.WaysToPayFormProvider
import models.FeeResponse
import models.payments.CreateBacsPaymentRequest
import models.payments.WaysToPay._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, AuthorisedRequest, DeclarationHelper}
import views.html.payments.WaysToPayView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class WaysToPayController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val statusService: StatusService,
  val paymentsService: PaymentsService,
  val authEnrolmentsService: AuthEnrolmentsService,
  val feeResponseService: FeeResponseService,
  val cc: MessagesControllerComponents,
  val renewalService: RenewalService,
  formProvider: WaysToPayFormProvider,
  view: WaysToPayView
) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      subHeading <- DeclarationHelper.getSubheadingBasedOnStatus(
                      request.credId,
                      request.amlsRefNumber,
                      request.accountTypeId,
                      statusService,
                      renewalService
                    )
    } yield Ok(view(formProvider(), subHeading))) getOrElse InternalServerError("Failed to retrieve data.")
  }

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          (for {
            subHeading <- DeclarationHelper.getSubheadingBasedOnStatus(
                            request.credId,
                            request.amlsRefNumber,
                            request.accountTypeId,
                            statusService,
                            renewalService
                          )
          } yield BadRequest(view(formWithErrors, subHeading))) getOrElse InternalServerError(
            "Failed to retrieve data."
          ),
        {
          case Card =>
            progressToPayment { (fees, paymentReference, safeId) =>
              paymentsService
                .requestPaymentsUrl(
                  fees,
                  controllers.routes.PaymentConfirmationController.paymentConfirmation(paymentReference).url,
                  fees.amlsReferenceNumber,
                  safeId,
                  request.accountTypeId
                )
                .map { nextUrl =>
                  Redirect(nextUrl.value)
                }
            }("Cannot retrieve payment information")
          case Bacs =>
            progressToPayment { (fees, paymentReference, safeId) =>
              paymentsService
                .createBacsPayment(
                  CreateBacsPaymentRequest(
                    fees.amlsReferenceNumber,
                    paymentReference,
                    safeId,
                    paymentsService.amountFromSubmissionData(fees).fold(0)(_.map(_ * 100).value.toInt)
                  ),
                  request.accountTypeId
                )
                .map { _ =>
                  Redirect(controllers.payments.routes.TypeOfBankController.get())
                }
            }("Unable to save BACS info")
        }
      )
  }

  def progressToPayment(
    fn: (FeeResponse, String, String) => Future[Result]
  )(errorMessage: String)(implicit hc: HeaderCarrier, request: AuthorisedRequest[_]): Future[Result] = {

    val submissionDetails = for {
      amlsRefNo           <- OptionT(authEnrolmentsService.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier))
      (_, detailedStatus) <-
        OptionT.liftF(statusService.getDetailedStatus(Option(amlsRefNo), request.accountTypeId, request.credId))
      fees                <- OptionT(feeResponseService.getFeeResponse(amlsRefNo, request.accountTypeId))
    } yield (fees, detailedStatus)

    submissionDetails.value flatMap {
      case Some((fees, Some(detailedStatus))) if fees.paymentReference.isDefined & detailedStatus.safeId.isDefined =>
        fn(fees, fees.paymentReference.get, detailedStatus.safeId.get)
      case _                                                                                                       => Future.successful(InternalServerError(errorMessage))
    }
  }
}
