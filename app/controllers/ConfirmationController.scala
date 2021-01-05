/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.{AmlsConnector, DataCacheConnector, KeystoreConnector, _}
import javax.inject.{Inject, Singleton}
import models.ResponseType.AmendOrVariationResponseType
import models.confirmation.Currency
import models.status._
import models.{FeeResponse, SubmissionRequestStatus}
import play.api.Logger
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import services.{AuthEnrolmentsService, StatusService, _}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, BusinessName, FeeHelper}
import views.html.confirmation._

import scala.concurrent.Future

@Singleton
class ConfirmationController @Inject()(authAction: AuthAction,
                                       val ds: CommonPlayDependencies,
                                       private[controllers] val keystoreConnector: KeystoreConnector,
                                       private[controllers] implicit val dataCacheConnector: DataCacheConnector,
                                       private[controllers] implicit val amlsConnector: AmlsConnector,
                                       private[controllers] implicit val statusService: StatusService,
                                       private[controllers] val authenticator: AuthenticatorConnector,
                                       private[controllers] val enrolmentService: AuthEnrolmentsService,
                                       private[controllers] val confirmationService: ConfirmationService,
                                       val cc: MessagesControllerComponents,
                                       val feeHelper: FeeHelper,
                                       confirm_renewal: confirm_renewal,
                                       confirm_amendvariation: confirm_amendvariation,
                                       confirmation_new: confirmation_new,
                                       confirmation_no_fee: confirmation_no_fee
                                      ) extends AmlsBaseController(ds, cc) {

  val prefix = "[ConfirmationController]"

  def get() = authAction.async {
    implicit request =>
      // $COVERAGE-OFF$
      Logger.debug(s"[$prefix] - begin get()...")
      // $COVERAGE-ON$
      for {
        _ <- authenticator.refreshProfile
        status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
        submissionRequestStatus <- dataCacheConnector.fetch[SubmissionRequestStatus](request.credId, SubmissionRequestStatus.key)
        result <- resultFromStatus(status, submissionRequestStatus, request.amlsRefNumber, request.accountTypeId, request.credId, request.groupIdentifier)
        _ <- keystoreConnector.setConfirmationStatus
      } yield result
  }

  private def showRenewalConfirmation(fees: FeeResponse, status: SubmissionStatus, submissionRequestStatus: Option[SubmissionRequestStatus], credId: String)
                                     (implicit request: Request[AnyContent]) = {

    confirmationService.isRenewalDefined(credId) map { isRenewalDefined =>
      if (isRenewalDefined) {
        Ok(confirm_renewal(fees.paymentReference,
          fees.totalFees,
          fees.toPay(status, submissionRequestStatus),
          controllers.payments.routes.WaysToPayController.get().url,
          submissionRequestStatus.fold[Boolean](false)(_.isRenewalAmendment.getOrElse(false))))
      } else {
        Ok(confirm_amendvariation(fees.paymentReference,
          fees.totalFees,
          fees.toPay(status, submissionRequestStatus),
          controllers.payments.routes.WaysToPayController.get().url))
      }
    }
  }

  private def showAmendmentVariationConfirmation(fees: FeeResponse, status: SubmissionStatus, submissionRequestStatus: Option[SubmissionRequestStatus])
                                                (implicit request: Request[AnyContent]) = {

    val amount = fees.toPay(status, submissionRequestStatus)

    Future.successful(Ok(confirm_amendvariation(fees.paymentReference,
      Currency(fees.totalFees),
      amount,
      controllers.payments.routes.WaysToPayController.get().url)))
  }

  private def resultFromStatus(status: SubmissionStatus, submissionRequestStatus: Option[SubmissionRequestStatus],
                               amlsRegistrationNumber: Option[String], accountTypeId: (String, String), credId: String, groupIdentifier: Option[String])
                              (implicit hc: HeaderCarrier, request: Request[AnyContent], statusService: StatusService): Future[Result] = {

    // $COVERAGE-OFF$
    Logger.debug(s"[$prefix][resultFromStatus] - Begin get fee response...)")
    // $COVERAGE-ON$

    feeHelper.retrieveFeeResponse(amlsRegistrationNumber, accountTypeId, groupIdentifier, prefix) flatMap {
      case Some(fees) if fees.paymentReference.isDefined && fees.toPay(status, submissionRequestStatus) > 0 =>
        // $COVERAGE-OFF$
        Logger.debug(s"[$prefix][resultFromStatus] - Fee found)")
        // $COVERAGE-ON$

        // $COVERAGE-OFF$
        Logger.debug(s"[$prefix][resultFromStatus] - fees: $fees")
        // $COVERAGE-ON$

        status match {
          case SubmissionReadyForReview | SubmissionDecisionApproved if fees.responseType equals AmendOrVariationResponseType =>
            showAmendmentVariationConfirmation(fees, status, submissionRequestStatus)
          case ReadyForRenewal(_) | RenewalSubmitted(_) =>
            showRenewalConfirmation(fees, status, submissionRequestStatus, credId)
          case _ => {
            Future.successful(Ok(confirmation_new(fees.paymentReference, fees.totalFees, controllers.payments.routes.WaysToPayController.get().url)))
          }
        }

      case _ =>
        // $COVERAGE-OFF$
        Logger.debug(s"[$prefix][resultFromStatus] - No fee found)")
        // $COVERAGE-ON$
        for {
          name <- BusinessName.getBusinessNameFromAmls(amlsRegistrationNumber, accountTypeId, credId).value
        } yield {
          Ok(confirmation_no_fee(name.get))
        }
    }
  }
}