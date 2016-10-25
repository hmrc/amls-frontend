package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessactivities.BusinessActivities
import models.status.{NotCompleted, SubmissionReady, SubmissionDecisionApproved, SubmissionStatus}
import services.StatusService
import views.html.businessactivities.summary

import scala.concurrent.Future

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  private[controllers] def statusService: StatusService

  def isLinkEditable(status: Future[SubmissionStatus]): Future[Boolean] = {
    status.map {
      case SubmissionReady | NotCompleted => true
      case _ => false
    }
  }

  def get = Authorised.async {
    implicit authContext => implicit request =>
      for {
        ba <- dataCache.fetch[BusinessActivities](BusinessActivities.key)
        showEdit <- isLinkEditable(statusService.getStatus)
      } yield ba match {
        case Some(data) => Ok(summary(data))
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
     }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService: StatusService = StatusService
}
