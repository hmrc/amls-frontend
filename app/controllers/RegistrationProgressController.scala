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

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import models.businessmatching.{BusinessActivity, BusinessMatching}
import models.registrationprogress.{Completed, TaskList, TaskRow, Updated}
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePerson
import models.status._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import services.businessmatching.{BusinessMatchingService, ServiceFlow}
import services.cache.Cache
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, BusinessName, ControllerHelper, DeclarationHelper}
import views.html.registrationamendment.RegistrationAmendmentView
import views.html.registrationprogress.RegistrationProgressView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationProgressController @Inject()(protected[controllers] val authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               protected[controllers] val dataCache: DataCacheConnector,
                                               protected[controllers] val enrolmentsService: AuthEnrolmentsService,
                                               implicit val statusService: StatusService,
                                               protected[controllers] val progressService: ProgressService,
                                               protected[controllers] val sectionsProvider: SectionsProvider,
                                               protected[controllers] val businessMatchingService: BusinessMatchingService,
                                               protected[controllers] val serviceFlow: ServiceFlow,
                                               val amlsConnector: AmlsConnector,
                                               implicit val renewalService: RenewalService,
                                               val cc: MessagesControllerComponents,
                                               registration_progress: RegistrationProgressView,
                                               registration_amendment: RegistrationAmendmentView) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction.async {
      implicit request =>
        renewalService.isRenewalFlow(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap {
          case true => Future.successful(Redirect(controllers.renewal.routes.RenewalProgressController.get))
          case _ =>
            (for {
              status <- OptionT.liftF(statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
              cache <- OptionT(dataCache.fetchAll(request.credId))
              completePreApp <- OptionT(preApplicationComplete(cache, status, request.amlsRefNumber))
              responsiblePeople <- OptionT.fromOption[Future](cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key))
              businessMatching <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
              newActivities <- getNewActivities(request.credId) orElse OptionT.some(Set.empty[BusinessActivity])
              statusInfo <- OptionT.liftF(statusService.getDetailedStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
              statusResponse <- OptionT.liftF(Future(statusInfo._2))
              maybeBusinessName <- OptionT.liftF(getBusinessName(request.credId, statusResponse.fold(none[String])(_.safeId), request.accountTypeId).value)
            } yield {
              businessMatching.reviewDetails map { reviewDetails =>

                val newTaskRows = sectionsProvider.taskRowsFromBusinessActivities(
                  newActivities, businessMatching.msbServices)(cache, messages.preferred(request)
                )
                val taskRows = sectionsProvider.taskRows(cache)
                val taskListToDisplay = TaskList(taskRows.filter(tr => tr.msgKey != BusinessMatching.messageKey) diff newTaskRows)
                val canEditPreapplication = Set(NotCompleted, SubmissionReady, SubmissionDecisionApproved).contains(status)
                val activities = businessMatching.activities.fold(Seq.empty[String])(_.businessActivities.map(_.getMessage()).toSeq)
                val hasCompleteNominatedOfficer = ControllerHelper.hasCompleteNominatedOfficer(Option(responsiblePeople))
                val nominatedOfficerName = ControllerHelper.completeNominatedOfficerTitleName(Option(responsiblePeople))

                var businessName = ""
                maybeBusinessName.map{ bn => businessName = bn}

                if (completePreApp) {
                  Ok(registration_amendment(
                    taskListToDisplay,
                    amendmentDeclarationAvailable(taskRows),
                    businessName,
                    activities,
                    canEditPreapplication,
                    Some(newTaskRows),
                    hasCompleteNominatedOfficer,
                    nominatedOfficerName
                  ))
                } else {
                  Ok(registration_progress(
                    taskListToDisplay,
                    declarationAvailable(taskRows),
                    businessName,
                    activities,
                    canEditPreapplication,
                    hasCompleteNominatedOfficer,
                    nominatedOfficerName
                  ))
                }
              } getOrElse InternalServerError("Unable to retrieve the business details")
            }) getOrElse Redirect(controllers.routes.LandingController.get())
        }
  }

  private def declarationAvailable(seq: Seq[TaskRow]): Boolean = {
    seq forall { row =>
      row.status == Completed || row.status == Updated
    }
  }

  private def amendmentDeclarationAvailable(sections: Seq[TaskRow]): Boolean = {

    sections.foldLeft((true, false)) { (acc, section) =>

      val (hasPreviousCompleted, hasPreviousChanged) = acc

      (hasPreviousCompleted && (section.status == Completed || section.status == Updated), hasPreviousChanged || section.hasChanged)

    } match {
      case (true, true) => true
      case _ => false
    }
  }

  private def preApplicationComplete(cache: Cache, status: SubmissionStatus, amlsRegistrationNumber: Option[String]): Future[Option[Boolean]] = {

    val preAppStatus: SubmissionStatus => Boolean = s => Set(NotCompleted, SubmissionReady).contains(s)

    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
    } yield (preAppStatus(status), bm.isComplete) match {
      case (_, true) | (false, _) =>
        Future.successful(amlsRegistrationNumber) map {
          case Some(_) => status match {
            case NotCompleted | SubmissionReady => Some(false)
            case _ => Some(true)
          }
          case None => Some(false)
        }
      case _ => Future.successful(None)
    }).getOrElse(Future.successful(None))
  }

  private def getNewActivities(cacheId: String): OptionT[Future, Set[BusinessActivity]] =
    businessMatchingService.getAdditionalBusinessActivities(cacheId)

  private def getBusinessName(credId: String, safeId: Option[String], accountTypeId: (String, String))(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    BusinessName.getName(credId, safeId, accountTypeId)(hc, ec, dataCache, amlsConnector)

  def post(): Action[AnyContent] = authAction.async {
    implicit request =>
      DeclarationHelper.promptRenewal(request.amlsRefNumber, request.accountTypeId, request.credId).flatMap {
        case true => Future.successful(Redirect(controllers.declaration.routes.RenewRegistrationController.get()))
        case false => progressService.getSubmitRedirect(request.amlsRefNumber, request.accountTypeId, request.credId) map {
          case Some(url) => Redirect(url)
          case _ => InternalServerError("Could not get data for redirect")
        }
      }
  }
}
