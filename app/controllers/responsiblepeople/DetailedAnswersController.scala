package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.ResponsiblePeople
import views.html.responsiblepeople._

trait DetailedAnswersController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get(index: Int, fromYourAnswers: Boolean) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCache.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
          case Some(data) => {
            data.lift(index - 1) match {
              case Some(x) => Ok(views.html.responsiblepeople.detailed_answers(Some(x), index, fromYourAnswers))
              case _ => NotFound(notFoundView)
            }
          }
          case _ => Redirect(controllers.routes.RegistrationProgressController.get())
        }
    }
}

object DetailedAnswersController extends DetailedAnswersController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
