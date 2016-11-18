package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.ResponsiblePeople
import views.html.responsiblepeople._

trait CheckYourAnswersController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
          case Some(data) => Ok(check_your_answers(data))
          case _ => Redirect(controllers.routes.RegistrationProgressController.get())
        }
    }
}

object CheckYourAnswersController extends CheckYourAnswersController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
