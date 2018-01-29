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
import config.{AMLSAuditConnector, AMLSAuthConnector}
import connectors._
import models.ResponseType.AmendOrVariationResponseType
import models.businessmatching.BusinessMatching
import models.confirmation.{BreakdownRow, Currency}
import models.payments._
import models.renewal.Renewal
import models.status._
import models.{FeeResponse, ReadStatusResponse}
import play.api.Play
import play.api.mvc.{AnyContent, Request, Result}
import services.{FeeResponseService, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.BusinessName
import views.html.confirmation._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ConfirmationController extends BaseController {

  private[controllers] val keystoreConnector: KeystoreConnector

  private[controllers] implicit val dataCacheConnector: DataCacheConnector

  private[controllers] implicit val amlsConnector: AmlsConnector

  private[controllers] val statusService: StatusService

  private[controllers] lazy val authenticator = Play.current.injector.instanceOf[AuthenticatorConnector]

  private[controllers] lazy val feeResponseService = Play.current.injector.instanceOf[FeeResponseService]

  private[controllers] lazy val authEnrolmentsService = Play.current.injector.instanceOf[AuthEnrolmentsService]

  private[controllers] lazy val paymentsConnector = Play.current.injector.instanceOf[PayApiConnector]

  private[controllers] lazy val paymentsService = Play.current.injector.instanceOf[PaymentsService]

  private[controllers] lazy val submissionResponseService = Play.current.injector.instanceOf[ConfirmationService]

  val auditConnector: AuditConnector

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          _ <- authenticator.refreshProfile
          status <- statusService.getStatus
          result <- resultFromStatus(status)
          _ <- keystoreConnector.setConfirmationStatus
        } yield result
  }

  def paymentConfirmation(reference: String) = Authorised.async {
    implicit authContext =>
      implicit request =>

        def companyNameT(maybeStatus: Option[ReadStatusResponse]) =
          maybeStatus.fold[OptionT[Future, String]](OptionT.some("")) { r => BusinessName.getName(r.safeId) }

        val msgFromPaymentStatus = Map[PaymentStatus, String](
          PaymentStatuses.Failed -> "confirmation.payment.failed.reason.failure",
          PaymentStatuses.Cancelled -> "confirmation.payment.failed.reason.cancelled"
        )

        val result = for {
          (status, detailedStatus) <- OptionT.liftF(statusService.getDetailedStatus)
          businessName <- companyNameT(detailedStatus) orElse OptionT.some("")
          renewalData <- OptionT.liftF(dataCacheConnector.fetch[Renewal](Renewal.key))
          paymentStatus <- OptionT.liftF(amlsConnector.refreshPaymentStatus(reference))
          payment <- OptionT(amlsConnector.getPaymentByPaymentReference(reference))
          _ <- doAudit(paymentStatus.currentStatus)
        } yield (status, paymentStatus.currentStatus) match {
          case (_, status@(PaymentStatuses.Failed | PaymentStatuses.Cancelled)) =>
            Ok(payment_failure(msgFromPaymentStatus(status), Currency(payment.amountInPence.toDouble / 100), reference))

          case (SubmissionReadyForReview | SubmissionDecisionApproved | RenewalSubmitted(_), _) =>
            Ok(payment_confirmation_amendvariation(businessName, reference))

          case (ReadyForRenewal(_), _) => if (renewalData.isDefined) {
            Ok(payment_confirmation_renewal(businessName, reference))
          } else {
            Ok(payment_confirmation_amendvariation(businessName, reference))
          }

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
        } yield Ok(views.html.confirmation.confirmation_bacs(name))

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

  private def showRenewalConfirmation(fees: FeeResponse, breakdownRows: Future[Option[Seq[BreakdownRow]]], status: SubmissionStatus)
                                     (implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {

    submissionResponseService.isRenewalDefined flatMap { isRenewalDefined =>
      breakdownRows map {
        case Some(rows) if fees.totalFees > 0 => {
          isRenewalDefined match {
            case true => Ok(confirm_renewal(fees.paymentReference, fees.totalFees, rows, fees.difference, controllers.payments.routes.WaysToPayController.get().url)).some
            case false => Ok(confirm_amendvariation(fees.paymentReference, fees.totalFees, rows, fees.difference, controllers.payments.routes.WaysToPayController.get().url)).some
          }
        }
        case _ => None
      }
    }
  }

  private def showAmendmentVariationConfirmation(fees: FeeResponse, breakdownRows: Future[Option[Seq[BreakdownRow]]])
                                                (implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {
    breakdownRows map {
      case Some(rows) =>
        Ok(confirm_amendvariation(fees.paymentReference, fees.totalFees, rows, fees.difference, controllers.payments.routes.WaysToPayController.get().url)).some
      case _ => None
    }
  }

  private def resultFromStatus(status: SubmissionStatus)
                              (implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]): Future[Result] = {

    OptionT.liftF(retrieveFeeResponse) flatMap {
      case Some(fees) if fees.paymentReference.isDefined =>

        val breakdownRows = submissionResponseService.getBreakdownRows(status, fees)

        status match {
          case SubmissionReadyForReview | SubmissionDecisionApproved if fees.responseType equals AmendOrVariationResponseType =>
            OptionT(showAmendmentVariationConfirmation(fees, breakdownRows))
          case ReadyForRenewal(_) | RenewalSubmitted(_) =>
            OptionT(showRenewalConfirmation(fees, breakdownRows, status))
          case _ =>
            OptionT.liftF(breakdownRows map {
              case Some(rows) =>
                Ok(confirmation_new(fees.paymentReference, fees.totalFees, rows, controllers.payments.routes.WaysToPayController.get().url))
            })
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

object ConfirmationController extends ConfirmationController {
  // $COVERAGE-OFF$
  override protected val authConnector = AMLSAuthConnector
  override val statusService: StatusService = StatusService
  override private[controllers] val keystoreConnector = KeystoreConnector
  override private[controllers] val dataCacheConnector = DataCacheConnector
  override private[controllers] val amlsConnector = AmlsConnector
  override val auditConnector = AMLSAuditConnector
}
