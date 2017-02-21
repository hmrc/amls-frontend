package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, Section}
import models.responsiblepeople.{NominatedOfficer, ResponsiblePeople}
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview, SubmissionStatus}
import play.api.mvc.{Action, AnyContent, Request}
import services.{AuthEnrolmentsService, ProgressService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import views.html.registrationamendment.registration_amendment
import views.html.registrationprogress.registration_progress
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


trait RegistrationProgressController extends BaseController {

  protected[controllers] def service: ProgressService

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
        if (AmendmentsToggle.feature) {
          getWithAmendments
        } else {
          getWithoutAmendments
        }
  }

  private def getWithAmendments(implicit hc: HeaderCarrier, ac: AuthContext, r: Request[_]) = {
    val x = dataCache.fetchAll
    x.flatMap { cacheMapO =>
      cacheMapO.map { cacheMap: CacheMap =>
        val sections = service.sections(cacheMap)
        preApplicationComplete(cacheMap) map {
          case Some(x) => x match {
            case true => Ok(registration_amendment(sections, amendmentDeclarationAvailable(sections)))
            case false => Ok(registration_progress(sections, declarationAvailable(sections)))
          }
          case None => Redirect(controllers.routes.LandingController.get())
        }
      }.getOrElse(Future.successful(Ok(registration_progress(Seq.empty[Section], false))))
    }
  }

  private def getWithoutAmendments(implicit hc: HeaderCarrier, ac: AuthContext, r: Request[_]) =
    service.sections map {
      sections => {
        Ok(registration_progress(sections, declarationAvailable(sections)))
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
          responsiblePeople <- dataCache.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key)
        } yield {
          responsiblePeople match {
            case Some(rps) => {
              val hasNominatedOfficer = rps.exists(_.positions.fold(false)(_.positions.contains(NominatedOfficer)))
              hasNominatedOfficer match {
                case true => redirectWhoIsRegistering(amendmentFlow)
                case false => redirectBusinessNominatedOfficer(amendmentFlow)
              }
            }
            case _ => redirectWhoIsRegistering(amendmentFlow)
          }
        }
  }
}

object RegistrationProgressController extends RegistrationProgressController {
  // $COVERAGE-OFF$
  override protected[controllers] val authConnector: AuthConnector = AMLSAuthConnector
  override protected[controllers] val service = ProgressService
  override protected[controllers] val dataCache = DataCacheConnector
  override protected[controllers] val enrolmentsService = AuthEnrolmentsService
  override protected[controllers] val statusService = StatusService
}
