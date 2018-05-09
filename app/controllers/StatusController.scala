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
import connectors.{AmlsConnector, AuthenticatorConnector, DataCacheConnector, _}
import javax.inject.{Inject, Singleton}
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.responsiblepeople.ResponsiblePeople
import models.status._
import models.withdrawal.WithdrawalStatus
import models.{FeeResponse, ReadStatusResponse}
import org.joda.time.LocalDate
import play.api.mvc.{AnyContent, Request, Result}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{BusinessName, ControllerHelper}
import views.html.status._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatusController @Inject()(val landingService: LandingService,
                                  val statusService: StatusService,
                                  val enrolmentsService: AuthEnrolmentsService,
                                  val feeConnector: FeeConnector,
                                  val renewalService: RenewalService,
                                  val progressService: ProgressService,
                                  val amlsConnector: AmlsConnector,
                                  val dataCache: DataCacheConnector,
                                  val authenticator: AuthenticatorConnector,
                                  val authConnector: AuthConnector = AMLSAuthConnector,
                                 val feeResponseService: FeeResponseService
                                 ) extends BaseController {

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
          bm <- dataCache.fetch[BusinessMatching](BusinessMatching.key)
          maybeActivities <- Future(bm.activities)
          page <- if (withdrawalStatus.contains(WithdrawalStatus(true))) {
            Future.successful(getDecisionPage(mlrRegNumber, (SubmissionWithdrawn, None), maybeBusinessName, responsiblePeople, maybeActivities))
          } else {
            getPageBasedOnStatus(
              mlrRegNumber,
              statusInfo,
              maybeBusinessName,
              feeResponse,
              fromDuplicateSubmission,
              responsiblePeople,
              maybeActivities
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
      case (Some(mlNumber), (SubmissionReadyForReview | SubmissionDecisionApproved)) => feeResponseService.getFeeResponse(mlNumber)
      case _ => Future.successful(None)
    }
  }


  def newSubmission = Authorised.async {
    implicit authContext =>
      implicit request => {
        val redirect = for {
          amlsRegNumber <- OptionT(enrolmentsService.amlsRegistrationNumber)
          _ <- OptionT.liftF(enrolmentsService.deEnrol(amlsRegNumber))
          _ <- OptionT.liftF(authenticator.refreshProfile)
          _ <- OptionT.liftF(dataCache.remove(authContext.user.oid))
        } yield Redirect(controllers.routes.LandingController.start(true))

        redirect getOrElse InternalServerError("New submission failed")
      }
  }

  private def getPageBasedOnStatus(mlrRegNumber: Option[String],
                                   statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                                   businessNameOption: Option[String],
                                   feeResponse: Option[FeeResponse],
                                   fromDuplicateSubmission: Boolean,
                                   responsiblePeople: Option[Seq[ResponsiblePeople]],
                                   activities: Option[BusinessActivities])
                                  (implicit request: Request[AnyContent], authContext: AuthContext) = {
    statusInfo match {
      case (NotCompleted, _) | (SubmissionReady, _) | (SubmissionReadyForReview, _) =>
        getInitialSubmissionPage(mlrRegNumber, statusInfo._1, businessNameOption, feeResponse, fromDuplicateSubmission)
      case (SubmissionDecisionApproved, _) | (SubmissionDecisionRejected, _) |
           (SubmissionDecisionRevoked, _) | (SubmissionDecisionExpired, _) |
           (SubmissionWithdrawn, _) | (DeRegistered, _) =>
        Future.successful(getDecisionPage(mlrRegNumber, statusInfo, businessNameOption, responsiblePeople, activities))
      case (ReadyForRenewal(_), _) | (RenewalSubmitted(_), _) =>
        getRenewalFlowPage(mlrRegNumber, statusInfo, businessNameOption, responsiblePeople, activities)
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
                              responsiblePeople: Option[Seq[ResponsiblePeople]],
                              maybeActivities: Option[BusinessActivities])(implicit request: Request[AnyContent]) = {
    statusInfo match {
      case (SubmissionDecisionApproved, statusDtls) => {
        val endDate = statusDtls.fold[Option[LocalDate]](None)(_.currentRegYearEndDate)
        val activities = maybeActivities.map(_.businessActivities.map(_.getMessage)) getOrElse Set.empty

        Ok {
          //noinspection ScalaStyle
          status_supervised(
            mlrRegNumber.getOrElse(""),
            businessNameOption,
            endDate,
            renewalFlow = false,
            ControllerHelper.nominatedOfficerTitleName(responsiblePeople),
            activities
          )
        }
      }

      case (SubmissionDecisionRejected, _) => Ok(status_rejected(mlrRegNumber.getOrElse(""), businessNameOption, ApplicationConfig.allowReregisterToggle))
      case (SubmissionDecisionRevoked, _) => Ok(status_revoked(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionDecisionExpired, _) => Ok(status_expired(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionWithdrawn, _) => Ok(status_withdrawn(businessNameOption))
      case (DeRegistered, _) =>
        val deregistrationDate = for {
          info <- statusInfo._2
          date <- info.deRegistrationDate
        } yield date

        Ok(status_deregistered(businessNameOption, deregistrationDate, ApplicationConfig.allowReregisterToggle))
    }
  }

  private def getRenewalFlowPage(mlrRegNumber: Option[String],
                                 statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                                 businessNameOption: Option[String],
                                 responsiblePeople: Option[Seq[ResponsiblePeople]],
                                 maybeActivities: Option[BusinessActivities])
                                (implicit request: Request[AnyContent],
                                 authContext: AuthContext) = {

    val activities = maybeActivities.map(_.businessActivities.map(_.getMessage)) getOrElse Set.empty

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
              ControllerHelper.nominatedOfficerTitleName(responsiblePeople),
              activities
            )))
        }
      }
    }
  }

  private def getBusinessName(maybeSafeId: Option[String])(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext) =
    BusinessName.getName(maybeSafeId)(hc, ac, ec, dataCache, amlsConnector)
}


