package controllers.aboutyou

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import models.aboutyou.{AboutYou, RoleWithinBusiness}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait RoleWithinBusinessController extends FrontendController with Actions {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[RoleWithinBusiness](AboutYou.key) map {
        case Some(data) => Ok(views.html.role_within_business(Form2[RoleWithinBusiness](data), edit))
        case _ => Ok(views.html.role_within_business(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request => {
      Form2[RoleWithinBusiness](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.role_within_business(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutYou <- dataCacheConnector.fetchDataShortLivedCache[AboutYou](AboutYou.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[AboutYou](AboutYou.key,
              aboutYou.roleWithinBusiness(data)
            )
          } yield Redirect(routes.SummaryController.get())
      }
    }
  }
}

object RoleWithinBusinessController extends RoleWithinBusinessController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
