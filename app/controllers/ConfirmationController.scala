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

package controllers

import connectors._
import models.ResponseType.AmendOrVariationResponseType
import models.status._
import models.{FeeResponse, SubmissionRequestStatus}
import play.api.Logging
import play.api.mvc._
import services.{ConfirmationService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, BusinessName, FeeHelper}
import views.html.confirmation.{ConfirmationAmendmentView, ConfirmationNewView, ConfirmationNoFeeView, ConfirmationRenewalView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ConfirmationController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  private[controllers] implicit val dataCacheConnector: DataCacheConnector,
  private[controllers] implicit val amlsConnector: AmlsConnector,
  private[controllers] implicit val statusService: StatusService,
  private[controllers] val confirmationService: ConfirmationService,
  val cc: MessagesControllerComponents,
  val feeHelper: FeeHelper,
  confirmationRenewal: ConfirmationRenewalView,
  confirmationAmendment: ConfirmationAmendmentView,
  confirmationNew: ConfirmationNewView,
  confirmationNoFee: ConfirmationNoFeeView
) extends AmlsBaseController(ds, cc)
    with Logging {

  val prefix = "[ConfirmationController]"

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    // $COVERAGE-OFF$
    logger.debug(s"[$prefix] - begin get()...")
    // $COVERAGE-ON$
    for {
      status                  <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
      submissionRequestStatus <-
        dataCacheConnector.fetch[SubmissionRequestStatus](request.credId, SubmissionRequestStatus.key)
      result                  <- resultFromStatus(
                                   status,
                                   submissionRequestStatus,
                                   request.amlsRefNumber,
                                   request.accountTypeId,
                                   request.credId,
                                   request.groupIdentifier
                                 )
    } yield result
  }

  private def showRenewalConfirmation(
    fees: FeeResponse,
    status: SubmissionStatus,
    submissionRequestStatus: Option[SubmissionRequestStatus],
    credId: String
  )(implicit request: Request[AnyContent]): Future[Result] =
    confirmationService.isRenewalDefined(credId) map { isRenewalDefined =>
      if (isRenewalDefined) {
        Ok(
          confirmationRenewal(
            fees.paymentReference,
            fees.toPay(status, submissionRequestStatus),
            controllers.payments.routes.WaysToPayController.get().url,
            submissionRequestStatus.fold[Boolean](false)(_.isRenewalAmendment.getOrElse(false))
          )
        )
      } else {
        Ok(
          confirmationAmendment(
            fees.paymentReference,
            fees.toPay(status, submissionRequestStatus),
            controllers.payments.routes.WaysToPayController.get().url
          )
        )
      }
    }

  private def showAmendmentVariationConfirmation(
    fees: FeeResponse,
    status: SubmissionStatus,
    submissionRequestStatus: Option[SubmissionRequestStatus]
  )(implicit request: Request[AnyContent]): Future[Result] = {

    val amount = fees.toPay(status, submissionRequestStatus)

    Future.successful(
      Ok(
        confirmationAmendment(fees.paymentReference, amount, controllers.payments.routes.WaysToPayController.get().url)
      )
    )
  }

  private def resultFromStatus(
    status: SubmissionStatus,
    submissionRequestStatus: Option[SubmissionRequestStatus],
    amlsRegistrationNumber: Option[String],
    accountTypeId: (String, String),
    credId: String,
    groupIdentifier: Option[String]
  )(implicit hc: HeaderCarrier, request: Request[AnyContent], statusService: StatusService): Future[Result] = {

    // $COVERAGE-OFF$
    logger.debug(s"[$prefix][resultFromStatus] - Begin get fee response...)")
    // $COVERAGE-ON$

    feeHelper.retrieveFeeResponse(amlsRegistrationNumber, accountTypeId, groupIdentifier, prefix) flatMap {
      case Some(fees) if fees.paymentReference.isDefined && fees.toPay(status, submissionRequestStatus) > 0 =>
        // $COVERAGE-OFF$
        logger.debug(s"[$prefix][resultFromStatus] - Fee found)")
        // $COVERAGE-ON$

        // $COVERAGE-OFF$
        logger.debug(s"[$prefix][resultFromStatus] - fees: $fees")
        // $COVERAGE-ON$

        status match {
          case SubmissionReadyForReview | SubmissionDecisionApproved
              if fees.responseType equals AmendOrVariationResponseType =>
            showAmendmentVariationConfirmation(fees, status, submissionRequestStatus)
          case ReadyForRenewal(_) | RenewalSubmitted(_) =>
            showRenewalConfirmation(fees, status, submissionRequestStatus, credId)
          case _                                        =>
            Future.successful(
              Ok(
                confirmationNew(
                  fees.paymentReference,
                  fees.totalFees,
                  controllers.payments.routes.WaysToPayController.get().url
                )
              )
            )
        }

      case _ =>
        // $COVERAGE-OFF$
        logger.debug(s"[$prefix][resultFromStatus] - No fee found)")
        // $COVERAGE-ON$
        BusinessName.getBusinessNameFromAmls(amlsRegistrationNumber, accountTypeId, credId).value.map { name =>
          Ok(confirmationNoFee(name))
        }
    }
  }
}
