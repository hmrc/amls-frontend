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

package controllers

import audit.PaymentConfirmationEvent
import cats.data.OptionT
import cats.implicits._
import config.AMLSAuthConnector
import connectors.{AmlsConnector, DataCacheConnector, KeystoreConnector, PayApiConnector, _}
import javax.inject.{Inject, Singleton}
import models.ResponseType.AmendOrVariationResponseType
import models.aboutthebusiness.{AboutTheBusiness, PreviouslyRegisteredYes}
import models.businessmatching.BusinessMatching
import models.confirmation.{BreakdownRow, Currency}
import models.payments._
import models.renewal.Renewal
import models.status._
import models.{FeeResponse, ReadStatusResponse, SubmissionRequestStatus}
import play.api.mvc.{AnyContent, Request, Result}
import services.{AuthEnrolmentsService, FeeResponseService, PaymentsService, StatusService, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BusinessName
import views.html.confirmation._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ConfirmationController @Inject()(
                                        val authConnector: AuthConnector = AMLSAuthConnector,
                                        private[controllers] val keystoreConnector: KeystoreConnector,
                                        private[controllers] implicit val dataCacheConnector: DataCacheConnector,
                                        private[controllers] implicit val amlsConnector: AmlsConnector,
                                        private[controllers] val statusService: StatusService,
                                        private[controllers] val authenticator: AuthenticatorConnector,
                                        private[controllers] val feeResponseService: FeeResponseService,
                                        private[controllers] val authEnrolmentsService: AuthEnrolmentsService,
                                        private[controllers] val paymentsConnector: PayApiConnector,
                                        private[controllers] val paymentsService: PaymentsService,
                                        private[controllers] val confirmationService: ConfirmationService,
                                        private[controllers] val auditConnector: AuditConnector
                                      ) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          _ <- authenticator.refreshProfile
          status <- statusService.getStatus
          submissionRequestStatus <- dataCacheConnector.fetch[SubmissionRequestStatus](SubmissionRequestStatus.key)
          result <- resultFromStatus(status, submissionRequestStatus)
          _ <- keystoreConnector.setConfirmationStatus
        } yield result
  }

  // scalastyle:off cyclomatic.complexity
  def paymentConfirmation(reference: String) = Authorised.async {
    implicit authContext =>
      implicit request =>

        def companyName(maybeStatus: Option[ReadStatusResponse]): OptionT[Future, String] =
          maybeStatus.fold[OptionT[Future, String]](OptionT.some("")) { r => BusinessName.getName(r.safeId) }

        val msgFromPaymentStatus = Map[String, String](
          "Failed" -> "confirmation.payment.failed.reason.failure",
          "Cancelled" -> "confirmation.payment.failed.reason.cancelled"
        )

        val paymentStatusFromQueryString = request.queryString.get("paymentStatus").map(_.head)

        val isPaymentSuccessful = !request.queryString.contains("paymentStatus")

        val result = for {
          (status, detailedStatus) <- OptionT.liftF(statusService.getDetailedStatus)
          businessName <- companyName(detailedStatus) orElse OptionT.some("")
          renewalData <- OptionT.liftF(dataCacheConnector.fetch[Renewal](Renewal.key))
          paymentStatus <- OptionT.liftF(amlsConnector.refreshPaymentStatus(reference))
          payment <- OptionT(amlsConnector.getPaymentByPaymentReference(reference))
          aboutTheBusiness <- OptionT(dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key))
          _ <- doAudit(paymentStatus.currentStatus)
        } yield (status, paymentStatus.currentStatus, isPaymentSuccessful) match {
          case (_, currentPaymentStatus, false) =>
            Ok(payment_failure(
              msgFromPaymentStatus(paymentStatusFromQueryString.getOrElse(currentPaymentStatus.toString)),
              Currency(payment.amountInPence.toDouble / 100), reference))

          case (SubmissionReadyForReview | SubmissionDecisionApproved | RenewalSubmitted(_), _, true) =>
            Ok(payment_confirmation_amendvariation(businessName, reference))

          case (ReadyForRenewal(_), _, true) => if (renewalData.isDefined) {
            Ok(payment_confirmation_renewal(businessName, reference))
          } else {
            Ok(payment_confirmation_amendvariation(businessName, reference))
          }

          case _ if aboutTheBusiness.previouslyRegistered.fold(false) {
            case PreviouslyRegisteredYes(_) => true
            case _ => false
          } => Ok(payment_confirmation_transitional_renewal(businessName, reference))

          case _ => Ok(payment_confirmation(businessName, reference))
        }

        result getOrElse InternalServerError("There was a problem trying to show the confirmation page")
  }

  def bacsConfirmation() = Authorised.async {
    implicit request =>
      implicit authContext =>
        val okResult = for {
          _ <- OptionT.liftF(authenticator.refreshProfile)
          refNo <- OptionT(authEnrolmentsService.amlsRegistrationNumber)
          status <- OptionT.liftF(statusService.getReadStatus(refNo))
          name <- BusinessName.getName(status.safeId)
          aboutTheBusiness <- OptionT(dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key))
        } yield () match {
          case _ if aboutTheBusiness.previouslyRegistered.fold(false) {
            case PreviouslyRegisteredYes(_) => true
            case _ => false
          } => Ok(views.html.confirmation.confirmation_bacs_transitional_renewal(name))
          case _ => Ok(views.html.confirmation.confirmation_bacs(name))
        }

        okResult getOrElse InternalServerError("Unable to get BACS confirmation")
  }

  def retryPayment = Authorised.async {
    implicit authContext =>
      implicit request =>
        val result = for {
          form <- OptionT.fromOption[Future](request.body.asFormUrlEncoded)
          paymentRef <- OptionT.fromOption[Future](form("paymentRef").headOption)
          oldPayment <- OptionT(amlsConnector.getPaymentByPaymentReference(paymentRef))
          newPayment <- OptionT.liftF(paymentsService.paymentsUrlOrDefault(
            paymentRef,
            oldPayment.amountInPence.toDouble / 100,
            controllers.routes.ConfirmationController.paymentConfirmation(paymentRef).url,
            oldPayment.amlsRefNo,
            oldPayment.safeId))
        } yield Redirect(newPayment.links.nextUrl)

        result getOrElse InternalServerError("Unable to retry payment due to a failure")
  }

  private def showRenewalConfirmation(
                                             fees: FeeResponse,
                                             breakdownRows: Future[Option[Seq[BreakdownRow]]],
                                             status: SubmissionStatus,
                                             submissionRequestStatus: Option[SubmissionRequestStatus]
                                     )
                                     (implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {

    confirmationService.isRenewalDefined flatMap { isRenewalDefined =>
      breakdownRows map {
        case maybeRows@Some(rows) if fees.toPay(status, submissionRequestStatus) > 0 =>
          if (isRenewalDefined) {
            Ok(confirm_renewal(fees.paymentReference,
              fees.totalFees,
              rows,
              fees.toPay(status, submissionRequestStatus),
              controllers.payments.routes.WaysToPayController.get().url)).some
          } else {
            Ok(confirm_amendvariation(fees.paymentReference,
              fees.totalFees,
              fees.toPay(status, submissionRequestStatus),
              maybeRows,
              controllers.payments.routes.WaysToPayController.get().url)).some
          }
        case _ => None
      }
    }
  }

  private def showAmendmentVariationConfirmation(
                                                        fees: FeeResponse,
                                                        breakdownRows: Future[Option[Seq[BreakdownRow]]],
                                                        status: SubmissionStatus,
                                                        submissionRequestStatus: Option[SubmissionRequestStatus])
                                                (implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {
    breakdownRows map { maybeRows =>
      val amount = fees.toPay(status, submissionRequestStatus)

      Ok(confirm_amendvariation(fees.paymentReference,
        Currency(fees.totalFees),
        amount,
        maybeRows,
        controllers.payments.routes.WaysToPayController.get().url)).some
    }
  }

  private def resultFromStatus(status: SubmissionStatus, submissionRequestStatus: Option[SubmissionRequestStatus])
                              (implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]): Future[Result] = {

    OptionT.liftF(retrieveFeeResponse) flatMap {
      case Some(fees) if fees.paymentReference.isDefined && fees.toPay(status, submissionRequestStatus) > 0 =>

        lazy val breakdownRows = confirmationService.getBreakdownRows(status, fees)

        status match {
          case SubmissionReadyForReview | SubmissionDecisionApproved if fees.responseType equals AmendOrVariationResponseType =>
            OptionT(showAmendmentVariationConfirmation(fees, breakdownRows, status, submissionRequestStatus))
          case ReadyForRenewal(_) | RenewalSubmitted(_) =>
            OptionT(showRenewalConfirmation(fees, breakdownRows, status, submissionRequestStatus))
          case _ =>
            OptionT.liftF(breakdownRows) map { maybeRows =>
              Ok(confirmation_new(fees.paymentReference, fees.totalFees, maybeRows, controllers.payments.routes.WaysToPayController.get().url))
            }
        }

      case _ => for {
        bm <- OptionT(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key))
      } yield Ok(confirmation_no_fee(bm.reviewDetails.get.businessName))

    } getOrElse InternalServerError("Could not determine a response")

  }

  private def doAudit(paymentStatus: PaymentStatus)(implicit hc: HeaderCarrier, ac: AuthContext) = {
    for {
      fees <- OptionT(retrieveFeeResponse)
      payRef <- OptionT.fromOption[Future](fees.paymentReference)
      result <- OptionT.liftF(auditConnector.sendEvent(PaymentConfirmationEvent(fees.amlsReferenceNumber, payRef, paymentStatus)))
    } yield result
  }

  private def retrieveFeeResponse(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[FeeResponse]] =
    (for {
      amlsRegistrationNumber <- OptionT(authEnrolmentsService.amlsRegistrationNumber)
      fees <- OptionT(feeResponseService.getFeeResponse(amlsRegistrationNumber))
    } yield fees).value

}