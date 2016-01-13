package controllers.aboutthebusiness

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import models.aboutthebusiness.{AboutTheBusiness, PreviouslyRegistered}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait BusinessRegisteredWithHMRCBeforeController extends FrontendController with Actions {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(AboutTheBusiness(Some(data), _)) => Ok(views.html.registered_with_HMRC_before(Form2[PreviouslyRegistered](data), edit))
        case _ => Ok(views.html.registered_with_HMRC_before(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request => {
      Form2[PreviouslyRegistered](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.registered_with_HMRC_before(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.previouslyRegistered(data)
            )
          } yield Redirect(routes.BusinessRegForVATController.get())
      }
    }
  }
}

object BusinessRegisteredWithHMRCBeforeController extends BusinessRegisteredWithHMRCBeforeController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}