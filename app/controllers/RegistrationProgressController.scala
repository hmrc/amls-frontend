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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, Section}
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePeople
import models.status._
import play.api.mvc.{Action, AnyContent, Request}
import services.{AuthEnrolmentsService, ProgressService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.ControllerHelper
import views.html.registrationamendment.registration_amendment
import views.html.registrationprogress.registration_progress

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RegistrationProgressController extends BaseController {

  protected[controllers] def progressService: ProgressService

  protected[controllers] def dataCache: DataCacheConnector

  protected[controllers] def enrolmentsService: AuthEnrolmentsService

  protected[controllers] def statusService: StatusService

  private def declarationAvailable(seq: Seq[Section]): Boolean =
    seq forall {
      _.status == Completed
    }

  private def amendmentDeclarationAvailable(sections: Seq[Section]) = {

    sections.foldLeft((true, false)) { (acc, s) =>
      (acc._1 && s.status == Completed,
        acc._2 || s.hasChanged)
    } match {
      case (true, true) => true
      case _ => false
    }
  }

  private def isRenewalFlow()(implicit hc: HeaderCarrier,
                              authContext: AuthContext,
                              request: Request[AnyContent]): Future[Boolean] = {
    statusService.getStatus flatMap {
      case ReadyForRenewal(_) | RenewalSubmitted(_) =>
        dataCache.fetch[Renewal](Renewal.key) map {
          case Some(_) => true
          case None => false
        }

      case _ => Future.successful(false)
    }
  }

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>

        isRenewalFlow flatMap {
          case true => Future.successful(Redirect(controllers.renewal.routes.RenewalProgressController.get()))
          case _ => {
            dataCache.fetchAll.flatMap {
              _.map { cacheMap =>
                val sections = progressService.sections(cacheMap)

                preApplicationComplete(cacheMap) map {
                  case Some(x) => x match {
                    case true => Ok(registration_amendment(sections.filter(_.name != BusinessMatching.messageKey), amendmentDeclarationAvailable(sections)))
                    case _ => Ok(registration_progress(sections, declarationAvailable(sections)))
                  }
                  case None => Redirect(controllers.routes.LandingController.get())
                }

              }.getOrElse(Future.successful(Ok(registration_progress(Seq.empty[Section], false))))
            }
          }
        }
  }

  private def preApplicationComplete(cache: CacheMap)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[Boolean]] = {
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
    } yield bm.isComplete match {
      case (true) => {
        enrolmentsService.amlsRegistrationNumber flatMap {
          case Some(_) => statusService.getStatus map {
            case  NotCompleted | SubmissionReady => Some(false)
            case _ => Some(true)
          }
          case None => Future.successful(Some(false))
        }
      }
      case _ => Future.successful(None)
    }).getOrElse(Future.successful(None))
  }

  def redirectWithNominatedOfficer(status: SubmissionStatus) = {
    status match {
      case SubmissionReady | NotCompleted => Redirect(routes.FeeGuidanceController.get())
      case SubmissionReadyForReview => Redirect(declaration.routes.WhoIsRegisteringController.get())
      case ReadyForRenewal(_) => Redirect(declaration.routes.WhoIsRegisteringController.getWithRenewal())
      case _ => Redirect(declaration.routes.WhoIsRegisteringController.getWithAmendment())
    }
  }

  def redirectWithoutNominatedOfficer(status: SubmissionStatus) = {
    status match {
      case SubmissionReady | NotCompleted | SubmissionReadyForReview => Redirect(declaration.routes.WhoIsTheBusinessNominatedOfficerController.get())
      case _ => Redirect(declaration.routes.WhoIsTheBusinessNominatedOfficerController.getWithAmendment())
    }
  }

  def post: Action[AnyContent] = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          status <- statusService.getStatus
          hasNominatedOfficer <- ControllerHelper.hasNominatedOfficer(dataCache.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key))
        } yield hasNominatedOfficer match {
          case true => redirectWithNominatedOfficer(status)
          case false => redirectWithoutNominatedOfficer(status)
        }
  }
}

object RegistrationProgressController extends RegistrationProgressController {
  // $COVERAGE-OFF$
  override protected[controllers] val authConnector: AuthConnector = AMLSAuthConnector
  override protected[controllers] val progressService = ProgressService
  override protected[controllers] val dataCache = DataCacheConnector
  override protected[controllers] val enrolmentsService = AuthEnrolmentsService
  override protected[controllers] val statusService = StatusService
}
