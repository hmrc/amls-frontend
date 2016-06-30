package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.hvd.Hvd
import views.html.hvd.summary

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get = HvdToggle {
    Authorised.async {
      implicit authContext => implicit request =>
        dataCache.fetch[Hvd](Hvd.key) map {
          case Some(data) => Ok(summary(data))
          case _ => Redirect(controllers.routes.RegistrationProgressController.get())
        }
    }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
