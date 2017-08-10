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

trait ConfirmationController extends BaseController {

  private[controllers] def submissionResponseService: SubmissionResponseService

  private[controllers] val keystoreConnector: KeystoreConnector

  private[controllers] implicit val dataCacheConnector: DataCacheConnector

  private[controllers] implicit val amlsConnector: AmlsConnector

  private[controllers] val authEnrolmentsService: AuthEnrolmentsService

  private[controllers] lazy val paymentsConnector = Play.current.injector.instanceOf[PayApiConnector]

  private[controllers] lazy val paymentsService = Play.current.injector.instanceOf[PaymentsService]

  val statusService: StatusService

  type ViewData = (String, Currency, Seq[BreakdownRow], Option[Currency])

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          status <- statusService.getStatus
          result <- resultFromStatus(status)
          _ <- keystoreConnector.setConfirmationStatus
        } yield result
  }

  def paymentConfirmation(reference: String) = Authorised.async {
    implicit authContext =>
      implicit request =>

        def companyNameT(maybeStatus: Option[ReadStatusResponse]) =
          maybeStatus.fold[OptionT[Future, String]](OptionT.some("")){ r => BusinessName.getName(r.safeId) }

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

          case (ReadyForRenewal(_), _) => if(renewalData.isDefined) {
            Ok(payment_confirmation_renewal(businessName, reference))
          } else {
            Ok(payment_confirmation_amendvariation(businessName, reference))
          }

          case _ => Ok(payment_confirmation(businessName, reference))
        }

        result getOrElse InternalServerError("There was a problem trying to show the confirmation page")
  }

  def retryPayment = Authorised.async {
    implicit authContext => implicit request =>
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
     dataCacheConnector.fetch[Renewal](Renewal.key).map ( _.isDefined)
  }

  private def getVariationOrRenewalFees(implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {
    getRenewalOrVariationData(
      isRenewalDefined flatMap {
        case true => submissionResponseService.getRenewal
        case false => submissionResponseService.getVariation
      }
    )
  }

  private def showRenewalConfirmation(implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {
    for {
      fees@(payRef, total, rows, _) <- OptionT(getVariationOrRenewalFees)
      amlsRefNo <- OptionT(authEnrolmentsService.amlsRegistrationNumber)
      paymentsRedirect <- OptionT.liftF(paymentsService.requestPaymentsUrl(fees, routes.ConfirmationController.paymentConfirmation(payRef).url, amlsRefNo))
      renewalDefined <- OptionT.liftF(isRenewalDefined)
    } yield {
      renewalDefined match {
        case true => Ok(confirm_renewal(payRef, total, rows, Some(total), paymentsRedirect.links.nextUrl))
        case false => Ok(confirm_amendvariation(payRef, total, rows, Some(total), paymentsRedirect.links.nextUrl))
      }
    }
  }

  private def showPostSubmissionConfirmation(getFees: Future[Option[ViewData]], status: SubmissionStatus)
                                            (implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {
    for {
      fees@(payRef, total, rows, difference) <- OptionT(getFees)
      amlsRefNo <- OptionT(authEnrolmentsService.amlsRegistrationNumber)
      paymentsRedirect <- OptionT.liftF(paymentsService.requestPaymentsUrl(fees, routes.ConfirmationController.paymentConfirmation(payRef).url, amlsRefNo))
    } yield {
      val feeToPay = status match {
        case SubmissionReadyForReview | RenewalSubmitted(_) => difference
        case _ => Some(total)
      }
      Ok(confirm_amendvariation(payRef, total, rows, feeToPay, paymentsRedirect.links.nextUrl))
    }
  }

  private def resultFromStatus(status: SubmissionStatus)(implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {

    val maybeResult = status match {
      case SubmissionReadyForReview => showPostSubmissionConfirmation(getAmendmentFees, status)
      case SubmissionDecisionApproved => showPostSubmissionConfirmation(getVariationOrRenewalFees, status)
      case ReadyForRenewal(_) | RenewalSubmitted(_) => showRenewalConfirmation
      case _ =>
        for {
          (paymentRef, total, rows, amlsRefNo) <- OptionT.liftF(submissionResponseService.getSubscription)
          paymentsRedirect <- OptionT.liftF(paymentsService.requestPaymentsUrl(
            (paymentRef, total, rows, None),
            routes.ConfirmationController.paymentConfirmation(paymentRef).url,
            amlsRefNo
          ))
        } yield {
          ApplicationConfig.paymentsUrlLookupToggle match {
            case true => Ok(confirmation_new(paymentRef, total, rows, paymentsRedirect.links.nextUrl))
            case _ => Ok(confirmation(paymentRef, total, rows))
          }
        }
    }

    lazy val noFeeResult = for {
      bm <- OptionT(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key))
    } yield Ok(confirmation_no_fee(bm.reviewDetails.get.businessName))

    maybeResult orElse noFeeResult getOrElse InternalServerError("Could not determine a response")
  }

  private def getAmendmentFees(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[ViewData]] = {
    submissionResponseService.getAmendment flatMap {
      case Some((paymentRef, total, rows, difference)) =>
        Future.successful(
          (difference, paymentRef) match {
            case (Some(currency), Some(payRef)) if currency.value > 0 => Some((payRef, total, rows, difference))
            case _ => None
          }
        )
      case None => Future.failed(new Exception("Cannot get data from amendment submission"))
    }
  }


  private def getRenewalOrVariationData(getData: Future[Option[(Option[String], Currency, Seq[BreakdownRow], Option[Currency])]])
                                       (implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[ViewData]] = {
    getData flatMap {
      case Some((paymentRef, total, rows, difference)) => Future.successful(
        paymentRef match {
          case Some(payRef) if total.value > 0 => Some((payRef, total, rows, difference))
          case _ => None
      })
      case None => Future.failed(new Exception("Cannot get data from submission"))
    }
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
