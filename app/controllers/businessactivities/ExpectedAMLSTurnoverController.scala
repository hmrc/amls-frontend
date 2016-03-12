package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{ExpectedAMLSTurnover}
import models.businessactivities.{BusinessActivities, _}
import views.html.businessactivities._


import scala.concurrent.Future

trait ExpectedAMLSTurnoverController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[ExpectedAMLSTurnover] = (for {
            businessActivities <- response
            expectedTurnover <- businessActivities.expectedAMLSTurnover
          } yield Form2[ExpectedAMLSTurnover](expectedTurnover)).getOrElse(EmptyForm)
          Ok(expected_amls_turnover(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ExpectedAMLSTurnover](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(expected_amls_turnover(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.expectedAMLSTurnover(data)
            )
          } yield edit match {
            case true => Redirect(routes.WhatYouNeedController.get())
            case false => Redirect(routes.BusinessFranchiseController.get())
          }
      }
    }
  }
}

object ExpectedAMLSTurnoverController extends ExpectedAMLSTurnoverController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
