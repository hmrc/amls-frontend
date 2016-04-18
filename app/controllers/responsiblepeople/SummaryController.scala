package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.ResponsiblePeople
import views.html.responsiblepeople._

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          dataCache.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
            case Some(data) => {
              Ok(summary(data))
            }
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
