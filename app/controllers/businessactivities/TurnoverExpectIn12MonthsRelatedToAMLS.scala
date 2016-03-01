package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{TurnerOverExpectIn12MonthsRelatedToAMLS}
import models.businessactivities.{BusinessActivities, _}


import scala.concurrent.Future

trait TurnerOverExpectIn12MonthsRelatedToAMLSController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        case Some(BusinessActivities(_, Some(data) ,_)) => Ok(views.html.turnover_expect_in_12_months_related_to_amls(Form2[TurnerOverExpectIn12MonthsRelatedToAMLS](data), edit))
        case _ => Ok(views.html.turnover_expect_in_12_months_related_to_amls(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[TurnerOverExpectIn12MonthsRelatedToAMLS](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.turnover_expect_in_12_months_related_to_amls(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
              businessActivities.turnerOverExpectIn12MonthsRelatedToAMLS(data)
            )
          } yield edit match {
            case true => Redirect(routes.WhatYouNeedController.get())
            case false => Redirect(routes.WhatYouNeedController.get())
          }
      }
    }
  }
}

object TurnerOverExpectIn12MonthsRelatedToAMLSController extends TurnerOverExpectIn12MonthsRelatedToAMLSController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
