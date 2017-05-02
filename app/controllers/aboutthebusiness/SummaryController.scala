package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutthebusiness.AboutTheBusiness
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview}
import services.StatusService
import views.html.aboutthebusiness._

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector
  val statusService: StatusService

  def get = Authorised.async {
    implicit authContext => implicit request =>
      for {
        aboutTheBusiness <- dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
        status <- statusService.getStatus
      } yield aboutTheBusiness match {
        case Some(data) => {
          val showRegisteredForMLR = status match {
            case NotCompleted | SubmissionReady | SubmissionReadyForReview => true
            case _ => false
          }
          Ok(summary(data, showRegisteredForMLR))
        }
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}
