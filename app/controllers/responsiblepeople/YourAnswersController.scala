package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.ResponsiblePeople
import utils.RepeatingSection
import views.html.responsiblepeople.check_your_answers


trait YourAnswersController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(addOther: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
            case Some(data) => Ok(check_your_answers(data, addOther))
            case _ => Redirect(controllers.routes.RegistrationProgressController.get())
          }

      }
    }
}

object YourAnswersController extends YourAnswersController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
