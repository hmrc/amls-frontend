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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import javax.inject.{Inject, Singleton}
import models.businessmatching.{BusinessActivity, BusinessMatching}
import models.registrationprogress.{Completed, Section}
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePerson
import models.status._
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import services.businessmatching.{BusinessMatchingService, ServiceFlow}
import services.{AuthEnrolmentsService, ProgressService, RenewalService, SectionsProvider, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthAction, ControllerHelper, DeclarationHelper}
import views.html.registrationamendment.registration_amendment
import views.html.registrationprogress.registration_progress

import scala.concurrent.Future

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
                                               implicit val renewalService: RenewalService,
                                               val cc: MessagesControllerComponents,
                                               registration_progress: registration_progress,
                                               registration_amendment: registration_amendment) extends AmlsBaseController(ds, cc) {

  def get() = authAction.async {
      implicit request =>
        isRenewalFlow(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap {
          case true => Future.successful(Redirect(controllers.renewal.routes.RenewalProgressController.get))
          case _ =>
            (for {
              status <- OptionT.liftF(statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
              cacheMap <- OptionT(dataCache.fetchAll(request.credId))
              completePreApp <- OptionT(preApplicationComplete(cacheMap, status, request.amlsRefNumber))
              responsiblePeople <- OptionT.fromOption[Future](cacheMap.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key))
              businessMatching <- OptionT.fromOption[Future](cacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              newActivities <- getNewActivities(request.credId) orElse OptionT.some(Set.empty[BusinessActivity])
            } yield {
              businessMatching.reviewDetails map { reviewDetails =>
                val newSections = sectionsProvider.sectionsFromBusinessActivities(newActivities, businessMatching.msbServices)(cacheMap).toSeq
                val sections = sectionsProvider.sections(cacheMap)
                val sectionsToDisplay = sections.filter(s => s.name != BusinessMatching.messageKey) diff newSections
                val canEditPreapplication = Set(NotCompleted, SubmissionReady, SubmissionDecisionApproved).contains(status)
                val activities = businessMatching.activities.fold(Seq.empty[String])(_.businessActivities.map(_.getMessage()).toSeq)
                val hasCompleteNominatedOfficer = ControllerHelper.hasCompleteNominatedOfficer(Option(responsiblePeople))
                val nominatedOfficerName = ControllerHelper.completeNominatedOfficerTitleName(Option(responsiblePeople))

                completePreApp match {
                  case true => Ok(registration_amendment(
                    sectionsToDisplay,
                    amendmentDeclarationAvailable(sections),
                    reviewDetails.businessName,
                    activities,
                    canEditPreapplication,
                    Some(newSections),
                    hasCompleteNominatedOfficer,
                    nominatedOfficerName
                  ))
                  case _ => Ok(registration_progress(
                    sectionsToDisplay,
                    declarationAvailable(sections),
                    reviewDetails.businessName,
                    activities,
                    canEditPreapplication,
                    hasCompleteNominatedOfficer,
                    nominatedOfficerName
                  ))
                }
              } getOrElse InternalServerError("Unable to retrieve the business details")
            }) getOrElse Redirect(controllers.routes.LandingController.get)
        }
  }

  private def isRenewalFlow(amlsRegistrationNo: Option[String], accountTypeId: (String, String), cacheId: String)
                           (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Boolean] = {
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) flatMap {
      case ReadyForRenewal(_) =>
        dataCache.fetch[Renewal](cacheId, Renewal.key) map {
          case Some(_) => true
          case None => false
        }

      case _ => Future.successful(false)
    }
  }

  private def declarationAvailable(seq: Seq[Section]): Boolean =
    seq forall {
      _.status == Completed
    }

  private def amendmentDeclarationAvailable(sections: Seq[Section]) = {

    sections.foldLeft((true, false)) { (acc, section) =>

      val (hasPreviousCompleted, hasPreviousChanged) = acc

      (hasPreviousCompleted && section.status == Completed, hasPreviousChanged || section.hasChanged)

    } match {
      case (true, true) => true
      case _ => false
    }
  }

  private def preApplicationComplete(cache: CacheMap, status: SubmissionStatus, amlsRegistrationNumber: Option[String])
                                    (implicit hc: HeaderCarrier): Future[Option[Boolean]] = {

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

  private def getNewActivities(cacheId: String)(implicit hc: HeaderCarrier): OptionT[Future, Set[BusinessActivity]] =
    businessMatchingService.getAdditionalBusinessActivities(cacheId)

  def post() = authAction.async {
    implicit request =>
      DeclarationHelper.promptRenewal(request.amlsRefNumber, request.accountTypeId, request.credId).flatMap {
        case true => Future.successful(Redirect(controllers.declaration.routes.RenewRegistrationController.get))
        case false => progressService.getSubmitRedirect(request.amlsRefNumber, request.accountTypeId, request.credId) map {
          case Some(url) => Redirect(url)
          case _ => InternalServerError("Could not get data for redirect")
        }
      }
  }
}
