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

package controllers

import cats.data.OptionT
import cats.implicits._
import config.{AMLSAuthConnector, AppConfig}
import connectors.{AmlsConnector, AuthenticatorConnector, DataCacheConnector, _}
import javax.inject.{Inject, Singleton}
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.responsiblepeople.ResponsiblePerson
import models.status._
import models.{FeeResponse, ReadStatusResponse}
import org.joda.time.LocalDate
import play.api.mvc.{AnyContent, Request, Result}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthAction, BusinessName, ControllerHelper}
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
                                 authAction: AuthAction,
                                 val feeResponseService: FeeResponseService) extends DefaultBaseController {

  def get(fromDuplicateSubmission: Boolean = false) = authAction.async {
      implicit request =>
        for {
          refNo <- enrolmentsService.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier)
          statusInfo <- statusService.getDetailedStatus(refNo, request.accountTypeId, request.credId)
          statusResponse <- Future(statusInfo._2)
          maybeBusinessName <- getBusinessName(request.credId, statusResponse.fold(none[String])(_.safeId), request.accountTypeId).value
          feeResponse <- getFeeResponse(refNo, statusInfo._1, request.accountTypeId)
          responsiblePeople <- dataCache.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key)
          bm <- dataCache.fetch[BusinessMatching](request.credId, BusinessMatching.key)
          maybeActivities <- Future(bm.activities)
          page <- getPageBasedOnStatus(
              refNo,
              statusInfo,
              maybeBusinessName,
              feeResponse,
              fromDuplicateSubmission,
              responsiblePeople,
              maybeActivities,
              request.accountTypeId,
              request.credId)
        } yield page
  }

  def getFeeResponse(mlrRegNumber: Option[String], submissionStatus: SubmissionStatus, accountTypeId: (String, String))
                    (implicit headerCarrier: HeaderCarrier): Future[Option[FeeResponse]] = {

    (mlrRegNumber, submissionStatus) match {
      case (Some(mlNumber), (SubmissionReadyForReview | SubmissionDecisionApproved)) => feeResponseService.getFeeResponse(mlNumber, accountTypeId)
      case _ => Future.successful(None)
    }
  }

  def newSubmission = authAction.async {
      implicit request => {
        val redirect = for {
          amlsRegNumber <- OptionT.fromOption[Future](request.amlsRefNumber)
          _ <- OptionT.liftF(enrolmentsService.deEnrol(amlsRegNumber, request.groupIdentifier))
          _ <- OptionT.liftF(authenticator.refreshProfile)
          _ <- OptionT.liftF(dataCache.remove(request.credId))
        } yield Redirect(controllers.routes.LandingController.start(true))

        redirect getOrElse InternalServerError("New submission failed")
      }
  }

  private def getPageBasedOnStatus(mlrRegNumber: Option[String],
                                   statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                                   businessNameOption: Option[String],
                                   feeResponse: Option[FeeResponse],
                                   fromDuplicateSubmission: Boolean,
                                   responsiblePeople: Option[Seq[ResponsiblePerson]],
                                   activities: Option[BusinessActivities],
                                   accountTypeId: (String, String),
                                   cacheId: String)
                                  (implicit request: Request[AnyContent]) = {
    statusInfo match {
      case (NotCompleted, _) | (SubmissionReady, _) | (SubmissionReadyForReview, _) =>
        getInitialSubmissionPage(mlrRegNumber, statusInfo._1, businessNameOption, feeResponse, fromDuplicateSubmission, accountTypeId, cacheId)
      case (SubmissionDecisionApproved, _) | (SubmissionDecisionRejected, _) |
           (SubmissionDecisionRevoked, _) | (SubmissionDecisionExpired, _) |
           (SubmissionWithdrawn, _) | (DeRegistered, _) =>
        Future.successful(getDecisionPage(mlrRegNumber, statusInfo, businessNameOption, responsiblePeople, activities))
      case (ReadyForRenewal(_), _) | (RenewalSubmitted(_), _) =>
        getRenewalFlowPage(mlrRegNumber, statusInfo, businessNameOption, responsiblePeople, activities, cacheId)
      case (_, _) => Future.successful(Ok(status_incomplete(mlrRegNumber.getOrElse(""), businessNameOption)))
    }
  }

  private def getInitialSubmissionPage(mlrRegNumber: Option[String],
                                       status: SubmissionStatus,
                                       businessNameOption: Option[String],
                                       feeResponse: Option[FeeResponse],
                                       fromDuplicateSubmission: Boolean,
                                       accountTypeId: (String, String),
                                       cacheId: String)
                                      (implicit request: Request[AnyContent]): Future[Result] = {

    status match {
      case NotCompleted => Future.successful(Ok(status_incomplete(mlrRegNumber.getOrElse(""), businessNameOption)))
      case SubmissionReady => {
        OptionT(progressService.getSubmitRedirect(mlrRegNumber, accountTypeId, cacheId)) map (
          url =>
            Ok(status_not_submitted(mlrRegNumber.getOrElse(""), businessNameOption, url))
          ) getOrElse InternalServerError("Unable to get redirect data")
      }
      case _ =>
        Future.successful(
          Ok(status_submitted(mlrRegNumber.getOrElse(""),
          businessNameOption,
          feeResponse,
          fromDuplicateSubmission)))
    }
  }

  private def getDecisionPage(mlrRegNumber: Option[String],
                              statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                              businessNameOption: Option[String],
                              responsiblePeople: Option[Seq[ResponsiblePerson]],
                              maybeActivities: Option[BusinessActivities])(implicit request: Request[AnyContent]) = {
    statusInfo match {
      case (SubmissionDecisionApproved, statusDtls) => {
        val endDate = statusDtls.fold[Option[LocalDate]](None)(_.currentRegYearEndDate)
        val activities = maybeActivities.map(_.businessActivities.map(_.getMessage())) getOrElse Set.empty

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

      case (SubmissionDecisionRejected, _) =>
        Ok(status_rejected(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionDecisionRevoked, _) =>
        Ok(status_revoked(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionDecisionExpired, _) =>
        Ok(status_expired(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionWithdrawn, _) =>
        Ok(status_withdrawn(businessNameOption))
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
                                 responsiblePeople: Option[Seq[ResponsiblePerson]],
                                 maybeActivities: Option[BusinessActivities],
                                 cacheId: String)
                                (implicit request: Request[AnyContent]) = {

    val activities = maybeActivities.map(_.businessActivities.map(_.getMessage())) getOrElse Set.empty

    statusInfo match {
      case (RenewalSubmitted(renewalDate), _) =>
        Future.successful(Ok(status_renewal_submitted(
          mlrRegNumber.getOrElse(""),
          businessNameOption,
          renewalDate,
          ControllerHelper.nominatedOfficerTitleName(responsiblePeople)
        )))
      case (ReadyForRenewal(renewalDate), _) => {
        renewalService.getRenewal(cacheId) flatMap {
          case Some(renewal) =>
            renewalService.isRenewalComplete(renewal, cacheId) flatMap { complete =>
              if (complete) {
                Future.successful(Ok(status_renewal_not_submitted(
                  mlrRegNumber.getOrElse(""),
                  businessNameOption,
                  renewalDate,
                  ControllerHelper.nominatedOfficerTitleName(responsiblePeople)
                )))
              } else {
                Future.successful(Ok(status_renewal_incomplete(
                  mlrRegNumber.getOrElse(""),
                  businessNameOption,
                  renewalDate,
                  ControllerHelper.nominatedOfficerTitleName(responsiblePeople))))
              }
            }
          case _ => Future.successful(Ok(
            status_supervised(mlrRegNumber.getOrElse(""),
              businessNameOption,
              renewalDate,
              true,
              ControllerHelper.nominatedOfficerTitleName(responsiblePeople),
              activities)))
        }
      }
    }
  }

  private def getBusinessName(credId: String, safeId: Option[String], accountTypeId: (String, String))(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    BusinessName.getName(credId, safeId, accountTypeId)(hc, ec, dataCache, amlsConnector)
}


