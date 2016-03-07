package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.businessactivities.{NCARegistered, BusinessActivities}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait NCARegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key) map {
        case Some(BusinessActivities(_, _, _, _, _, _, Some(data),_)) =>
          Ok(views.html.nca_registered(Form2[NCARegistered](data), edit))
        case _ =>
          Ok(views.html.nca_registered(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[NCARegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.nca_registered(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
              businessActivities.ncaRegistered(data)
            )
          } yield edit match {
            case true => Redirect(routes.WhatYouNeedController.get())
            case false => Redirect(routes.ExpectedBusinessTurnoverController.get())
          }
      }
    }
  }
}

object NCARegisteredController extends NCARegisteredController {
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}