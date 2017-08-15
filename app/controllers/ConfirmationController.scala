/*
 * Copyright 2017 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits._
import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.{AmlsConnector, DataCacheConnector, KeystoreConnector, PayApiConnector}
import models.businessmatching.BusinessMatching
import models.confirmation.Currency._
import models.confirmation.{BreakdownRow, Currency}
import models.payments._
import models.renewal.Renewal
import models.status._
import models.ReadStatusResponse
import play.api.mvc.{AnyContent, Request}
import play.api.{Logger, Play}
import services.{AuthEnrolmentsService, PaymentsService, StatusService, SubmissionResponseService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.BusinessName
import views.html.confirmation._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Right

trait ConfirmationController extends BaseController {

  private[controllers] def submissionResponseService: SubmissionResponseService

  private[controllers] val keystoreConnector: KeystoreConnector

  private[controllers] implicit val dataCacheConnector: DataCacheConnector

  private[controllers] implicit val amlsConnector: AmlsConnector

  private[controllers] val authEnrolmentsService: AuthEnrolmentsService

  private[controllers] lazy val paymentsConnector = Play.current.injector.instanceOf[PayApiConnector]

  private[controllers] lazy val paymentsService = Play.current.injector.instanceOf[PaymentsService]

  val statusService: StatusService

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          status <- statusService.getStatus
          result <- resultFromStatus(status)
          _ <- keystoreConnector.setConfirmationStatus
        } yield result

  }
  type SubmissionData = (Option[String], Currency, Seq[BreakdownRow], Either[String, Option[Currency]])

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
          payment <- OptionT(amlsConnector.getPaymentByReference(reference))
        } yield (status, paymentStatus.currentStatus) match {
          case s@(_, PaymentStatuses.Failed | PaymentStatuses.Cancelled) =>
            Ok(payment_failure(msgFromPaymentStatus(s._2), Currency(payment.amountInPence.toDouble / 100), reference))

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

  def retryPayment = Authorised.async {
    implicit authContext =>
      implicit request =>
        val result = for {
          form <- OptionT.fromOption[Future](request.body.asFormUrlEncoded)
          paymentRef <- OptionT.fromOption[Future](form("paymentRef").headOption)
          oldPayment <- OptionT(amlsConnector.getPaymentByReference(paymentRef))
          amlsRefNumber <- OptionT.fromOption[Future](oldPayment.amlsRefNo) orElse OptionT(authEnrolmentsService.amlsRegistrationNumber)
          newPayment <- OptionT.liftF(paymentsService.paymentsUrlOrDefault(
              paymentRef,
              oldPayment.amountInPence.toDouble / 100,
              controllers.routes.ConfirmationController.paymentConfirmation(paymentRef).url,
              amlsRefNumber))
        } yield Redirect(newPayment.links.nextUrl)

        result getOrElse InternalServerError("Unable to retry payment due to a failure")
  }

  private def isRenewalDefined(implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]): Future[Boolean] = {
    dataCacheConnector.fetch[Renewal](Renewal.key).map(_.isDefined)
  }

  private def showRenewalConfirmation(getFees: Future[Option[SubmissionData]], status: SubmissionStatus)
                                     (implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {
    for {
      _@(Some(payRef), total, rows, _) <- OptionT(getFees)
      renewalDefined <- OptionT.liftF(isRenewalDefined)
    } yield {
      renewalDefined match {
        case true => Ok(confirm_renewal(payRef, total, rows, Some(total), controllers.payments.routes.WaysToPayController.get().url))
        case false => Ok(confirm_amendvariation(payRef, total, rows, Some(total), controllers.payments.routes.WaysToPayController.get().url))
      }
    }
  }

  private def showAmendmentVariationConfirmation(getFees: Future[Option[SubmissionData]], status: SubmissionStatus)
                                            (implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {
    OptionT(getFees map {
      case Some(_@(Some(payRef), total, rows, Right(Some(difference)))) if difference.value > 0 =>
        Ok(confirm_amendvariation(payRef, total, rows, total.some, controllers.payments.routes.WaysToPayController.get().url)).some
      case _ => None
    })
  }

  private def resultFromStatus(status: SubmissionStatus)(implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {

    val maybeResult = status match {
      case SubmissionReadyForReview | SubmissionDecisionApproved =>
        showAmendmentVariationConfirmation(submissionResponseService.getSubmissionData(status), status)
      case ReadyForRenewal(_) | RenewalSubmitted(_) =>
        showRenewalConfirmation(submissionResponseService.getSubmissionData(status), status)
      case _ => OptionT.liftF(submissionResponseService.getSubscription map {
        case (Some(paymentRef), total, rows, Left(_)) => {
          ApplicationConfig.paymentsUrlLookupToggle match {
            case true => Ok(confirmation_new(paymentRef, total, rows, controllers.payments.routes.WaysToPayController.get().url))
            case _ => Ok(confirmation(paymentRef, total, rows))
          }
        }
      })
    }

    lazy val noFeeResult = for {
      bm <- OptionT(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key))
    } yield Ok(confirmation_no_fee(bm.reviewDetails.get.businessName))

    maybeResult orElse noFeeResult getOrElse InternalServerError("Could not determine a response")
  }

}

object ConfirmationController extends ConfirmationController {
  // $COVERAGE-OFF$
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] val submissionResponseService = SubmissionResponseService
  override val statusService: StatusService = StatusService
  override private[controllers] val keystoreConnector = KeystoreConnector
  override private[controllers] val dataCacheConnector = DataCacheConnector
  override private[controllers] val amlsConnector = AmlsConnector
  override private[controllers] val authEnrolmentsService = AuthEnrolmentsService
}
