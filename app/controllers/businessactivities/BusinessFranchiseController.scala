package controllers.businessactivities

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessactivities.{BusinessActivities, _}

import scala.concurrent.Future

trait BusinessFranchiseController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            businessFranchise <- businessActivities.businessFranchise
          } yield Form2[BusinessFranchise](businessFranchise)).getOrElse(EmptyForm)
          Ok(views.html.business_franchise_name(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[BusinessFranchise](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.business_franchise_name(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
              businessActivities.businessFranchise(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.HowManyEmployeesController.get())
          }
      }
    }
  }
}

object BusinessFranchiseController extends BusinessFranchiseController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
