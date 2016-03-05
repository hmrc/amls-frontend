package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities._


import scala.concurrent.Future

trait ExpectedBusinessTurnoverController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        case Some(BusinessActivities(_, Some(data), _ ,_, _, _ , _)) => Ok(views.html.expected_business_turnover(Form2[ExpectedBusinessTurnover](data), edit))
        case _ => Ok(views.html.expected_business_turnover(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ExpectedBusinessTurnover](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.expected_business_turnover(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
              businessActivities.expectedBusinessTurnover(data)
            )
          } yield edit match {
            case true => Redirect(routes.WhatYouNeedController.get())
            case false => Redirect(routes.ExpectedAMLSTurnoverController.get())
          }
      }
    }
  }
}

object ExpectedBusinessTurnoverController extends ExpectedBusinessTurnoverController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
