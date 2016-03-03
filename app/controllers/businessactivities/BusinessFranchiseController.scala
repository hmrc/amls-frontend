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
        case Some(BusinessActivities(_, _, _, Some(data), _)) =>
          Ok(views.html.business_franchise_name(Form2[BusinessFranchise](data), edit))
        case _ =>
          Ok(views.html.business_franchise_name(EmptyForm, edit))
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
            case true => Redirect(routes.WhatYouNeedController.get())
            case false => Redirect(routes.WhatYouNeedController.get())
          }
      }
    }
  }
}

object BusinessFranchiseController extends BusinessFranchiseController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
