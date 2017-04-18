package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, Section}
import models.responsiblepeople.ResponsiblePeople
import models.status._
import play.api.mvc.{Action, AnyContent}
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

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>

        statusService.getStatus map { status =>
          println("=============status======================================================"+status)

        }
        statusService.getStatus flatMap {
          case ReadyForRenewal(_) => {
            Future.successful(Redirect(controllers.renewal.routes.RenewalProgressController.get()))
          }
          case _ => {
            dataCache.fetchAll.flatMap {
              _.map { cacheMap =>
                val sections = progressService.sections(cacheMap)

                preApplicationComplete(cacheMap) map {
                  case Some(x) => x match {
                    case true => Ok(registration_amendment(sections, amendmentDeclarationAvailable(sections)))
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
        enrolmentsService.amlsRegistrationNumber map {
          case Some(_) => Some(true)
          case None => Some(false)
        }
      }
      case _ => Future.successful(None)
    }).getOrElse(Future.successful(None))
  }

  def redirectWhoIsRegistering(amendmentFlow: Boolean) = {
    amendmentFlow match {
      case true => Redirect(declaration.routes.WhoIsRegisteringController.getWithAmendment())
      case false => Redirect(declaration.routes.WhoIsRegisteringController.get())
    }
  }

  def redirectBusinessNominatedOfficer(amendmentFlow: Boolean) = {
    amendmentFlow match {
      case true => Redirect(declaration.routes.WhoIsTheBusinessNominatedOfficerController.getWithAmendment())
      case false => Redirect(declaration.routes.WhoIsTheBusinessNominatedOfficerController.get())
    }
  }

  def isAmendment(status: Future[SubmissionStatus]) = {
    status map {
      case SubmissionReady | NotCompleted | SubmissionReadyForReview => false
      case _ => true
    }
  }

  def post: Action[AnyContent] = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          amendmentFlow <- isAmendment(statusService.getStatus)
          hasNominatedOfficer <- ControllerHelper.hasNominatedOfficer(dataCache.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key))
        } yield hasNominatedOfficer match {
          case true => redirectWhoIsRegistering(amendmentFlow)
          case false => redirectBusinessNominatedOfficer(amendmentFlow)
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
