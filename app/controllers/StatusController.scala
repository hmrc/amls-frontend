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

import cats.data.OptionT
import cats.implicits._
import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.{AmlsConnector, AuthenticatorConnector, DataCacheConnector, FeeConnector}
import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import models.deregister.{DeRegisterSubscriptionRequest, DeregistrationReason}
import models.responsiblepeople.ResponsiblePeople
import models.status._
import models.withdrawal.WithdrawalStatus
import models.{FeeResponse, ReadStatusResponse}
import org.joda.time.LocalDate
import play.api.Play
import play.api.mvc.{AnyContent, Request, Result}
import services._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AckRefGenerator, BusinessName, ControllerHelper}
import views.html.status._

import scala.concurrent.{ExecutionContext, Future}

trait StatusController extends BaseController {

  private[controllers] def landingService: LandingService

  private[controllers] def statusService: StatusService

  private[controllers] def enrolmentsService: AuthEnrolmentsService

  private[controllers] def feeConnector: FeeConnector

  private[controllers] def renewalService: RenewalService

  private[controllers] def progressService: ProgressService

  private[controllers] def amlsConnector: AmlsConnector

  protected[controllers] def dataCache: DataCacheConnector

  protected[controllers] def authenticator: AuthenticatorConnector

  def get(fromDuplicateSubmission: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          mlrRegNumber <- enrolmentsService.amlsRegistrationNumber
          statusInfo <- statusService.getDetailedStatus
          statusResponse <- Future(statusInfo._2)
          maybeBusinessName <- getBusinessName(statusResponse.fold(none[String])(_.safeId)).value
          feeResponse <- getFeeResponse(mlrRegNumber, statusInfo._1)
          withdrawalStatus <- dataCache.fetch[WithdrawalStatus](WithdrawalStatus.key)
          responsiblePeople <- dataCache.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key)
          page <- if (withdrawalStatus.contains(WithdrawalStatus(true))) {
            Future.successful(getDecisionPage(mlrRegNumber, (SubmissionWithdrawn, None), maybeBusinessName, responsiblePeople))
          } else {
            getPageBasedOnStatus(
              mlrRegNumber,
              statusInfo,
              maybeBusinessName,
              feeResponse,
              fromDuplicateSubmission,
              responsiblePeople
            )
          }
        } yield page
  }

  def withdraw = Authorised.async {
    implicit authContext => implicit request => ???
  }

  def getFeeResponse(mlrRegNumber: Option[String], submissionStatus: SubmissionStatus)(implicit authContext: AuthContext,
                                                                                       headerCarrier: HeaderCarrier): Future[Option[FeeResponse]] = {
    (mlrRegNumber, submissionStatus) match {
      case (Some(mlNumber), (SubmissionReadyForReview | SubmissionDecisionApproved)) => {
        feeConnector.feeResponse(mlNumber).map(x => x.responseType match {
          case AmendOrVariationResponseType if x.difference.fold(false)(_ > 0) => Some(x)
          case SubscriptionResponseType if x.totalFees > 0 => Some(x)
          case _ => None
        })
      }.recoverWith {
        case _: NotFoundException => Future.successful(None)
      }
      case _ => Future.successful(None)
    }
  }


  def newSubmission = Authorised.async {
    implicit authContext =>
      implicit request => {
        enrolmentsService.amlsRegistrationNumber flatMap { registrationString =>
          enrolmentsService.deEnrol(registrationString.getOrElse("")) flatMap { _ =>
            authenticator.refreshProfile flatMap { _ =>
              dataCache.remove(authContext.user.oid) map { _ =>
                Redirect(controllers.routes.LandingController.start(true))
              }
            }
          }
        }
      }
  }

  private def getPageBasedOnStatus(mlrRegNumber: Option[String],
                                   statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                                   businessNameOption: Option[String],
                                   feeResponse: Option[FeeResponse],
                                   fromDuplicateSubmission: Boolean,
                                   responsiblePeople: Option[Seq[ResponsiblePeople]])
                                  (implicit request: Request[AnyContent], authContext: AuthContext) = {
    statusInfo match {
      case (NotCompleted, _) | (SubmissionReady, _) | (SubmissionReadyForReview, _) =>
        getInitialSubmissionPage(mlrRegNumber, statusInfo._1, businessNameOption, feeResponse, fromDuplicateSubmission)
      case (SubmissionDecisionApproved, _) | (SubmissionDecisionRejected, _) |
           (SubmissionDecisionRevoked, _) | (SubmissionDecisionExpired, _) |
           (SubmissionWithdrawn, _) | (DeRegistered, _) =>
        Future.successful(getDecisionPage(mlrRegNumber, statusInfo, businessNameOption, responsiblePeople))
      case (ReadyForRenewal(_), _) | (RenewalSubmitted(_), _) =>
        getRenewalFlowPage(mlrRegNumber, statusInfo, businessNameOption, responsiblePeople)
      case (_, _) => Future.successful(Ok(status_incomplete(mlrRegNumber.getOrElse(""), businessNameOption)))
    }
  }

  private def getInitialSubmissionPage(mlrRegNumber: Option[String],
                                       status: SubmissionStatus,
                                       businessNameOption: Option[String],
                                       feeResponse: Option[FeeResponse], fromDuplicateSubmission: Boolean)
                                      (implicit request: Request[AnyContent], authContext: AuthContext): Future[Result] = {
    val isBacsPayment = for {
      amlsRegNo <- OptionT.fromOption[Future](mlrRegNumber)
      payment <- OptionT(amlsConnector.getPaymentByAmlsReference(amlsRegNo))
    } yield payment.isBacs.getOrElse(false)

    status match {
      case NotCompleted => Future.successful(Ok(status_incomplete(mlrRegNumber.getOrElse(""), businessNameOption)))
      case SubmissionReady => {
        OptionT(progressService.getSubmitRedirect) map (
          url =>
            Ok(status_not_submitted(mlrRegNumber.getOrElse(""), businessNameOption, url))
          ) getOrElse InternalServerError("Unable to get redirect data")
      }
      case _ => isBacsPayment.value map { maybeBacs =>
        Ok(status_submitted(mlrRegNumber.getOrElse(""),
          businessNameOption,
          feeResponse,
          fromDuplicateSubmission,
          showBacsContent = maybeBacs.getOrElse(false)))
      }
    }
  }

  private def getDecisionPage(mlrRegNumber: Option[String],
                              statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                              businessNameOption: Option[String],
                              responsiblePeople: Option[Seq[ResponsiblePeople]])(implicit request: Request[AnyContent]) = {
    statusInfo match {
      case (SubmissionDecisionApproved, statusDtls) =>
        val endDate = statusDtls.fold[Option[LocalDate]](None)(_.currentRegYearEndDate)

        Ok {
          //noinspection ScalaStyle
          status_supervised(
            mlrRegNumber.getOrElse(""),
            businessNameOption,
            endDate,
            renewalFlow = false,
            ControllerHelper.nominatedOfficerTitleName(responsiblePeople)
          )
        }

      case (SubmissionDecisionRejected, _) => Ok(status_rejected(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionDecisionRevoked, _) => Ok(status_revoked(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionDecisionExpired, _) => Ok(status_expired(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionWithdrawn, _) => Ok(status_withdrawn(businessNameOption))
      case (DeRegistered, _) =>
        val deregistrationDate = for {
          info <- statusInfo._2
          date <- info.deRegistrationDate
        } yield date

        Ok(status_deregistered(businessNameOption, deregistrationDate))
    }
  }

  private def getRenewalFlowPage(mlrRegNumber: Option[String],
                                 statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                                 businessNameOption: Option[String],
                                 responsiblePeople: Option[Seq[ResponsiblePeople]])
                                (implicit request: Request[AnyContent],
                                 authContext: AuthContext) = {

    statusInfo match {
      case (RenewalSubmitted(renewalDate), _) =>
        Future.successful(Ok(status_renewal_submitted(
          mlrRegNumber.getOrElse(""),
          businessNameOption,
          renewalDate,
          ControllerHelper.nominatedOfficerTitleName(responsiblePeople)
        )
        ))
      case (ReadyForRenewal(renewalDate), _) => {
        renewalService.getRenewal flatMap {
          case Some(r) =>
            renewalService.isRenewalComplete(r) flatMap { complete =>
              if (complete) {
                Future.successful(Ok(status_renewal_not_submitted(
                  mlrRegNumber.getOrElse(""),
                  businessNameOption,
                  renewalDate,
                  ControllerHelper.nominatedOfficerTitleName(responsiblePeople)
                )
                ))
              } else {
                Future.successful(Ok(status_renewal_incomplete(
                  mlrRegNumber.getOrElse(""),
                  businessNameOption,
                  renewalDate,
                  ControllerHelper.nominatedOfficerTitleName(responsiblePeople)
                )))
              }
            }
          case _ => Future.successful(Ok(
            status_supervised(mlrRegNumber.getOrElse(""),
              businessNameOption,
              renewalDate,
              true,
              ControllerHelper.nominatedOfficerTitleName(responsiblePeople)
            )))
        }
      }
    }
  }

  private def getBusinessName(maybeSafeId: Option[String])(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext) =
    BusinessName.getName(maybeSafeId)(hc, ac, ec, dataCache, amlsConnector)
}

object StatusController extends StatusController {
  // $COVERAGE-OFF$
  override private[controllers] val landingService: LandingService = LandingService
  override private[controllers] val statusService: StatusService = StatusService
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] lazy val enrolmentsService = Play.current.injector.instanceOf[AuthEnrolmentsService]
  override private[controllers] val feeConnector: FeeConnector = FeeConnector
  override private[controllers] val renewalService: RenewalService = Play.current.injector.instanceOf[RenewalService]
  override private[controllers] val progressService: ProgressService = Play.current.injector.instanceOf[ProgressService]
  override protected[controllers] val authenticator = Play.current.injector.instanceOf[AuthenticatorConnector]
  override protected[controllers] val dataCache = DataCacheConnector
  override private[controllers] val amlsConnector = AmlsConnector
  // $COVERAGE-ON$

}


