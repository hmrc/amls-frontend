package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutyou.{AboutYou, RoleWithinBusiness}

import scala.concurrent.Future

trait RoleWithinBusinessController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[AboutYou](AboutYou.key) map {
        case Some(AboutYou(_, Some(data))) => Ok(views.html.role_within_business(Form2[RoleWithinBusiness](data), edit))
        case _ => Ok(views.html.role_within_business(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[RoleWithinBusiness](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.role_within_business(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutYou <- dataCacheConnector.fetchDataShortLivedCache[AboutYou](AboutYou.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[AboutYou](AboutYou.key,
              aboutYou.roleWithinBusiness(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.YourDetailsController.get())
          }
      }
    }
  }
}

object RoleWithinBusinessController extends RoleWithinBusinessController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
