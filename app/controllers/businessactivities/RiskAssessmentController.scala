package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, RiskAssessmentPolicy}
import views.html.businessactivities._

import scala.concurrent.Future

trait RiskAssessmentController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[RiskAssessmentPolicy] = (for {
            businessActivities <- response
            riskAssessmentPolicy <- businessActivities.riskAssessmentPolicy
          } yield Form2[RiskAssessmentPolicy](riskAssessmentPolicy)).getOrElse(EmptyForm)
          Ok(risk_assessment_policy(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    import play.api.data.mapping.forms.Rules._
    implicit authContext => implicit request =>
      Form2[RiskAssessmentPolicy](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(risk_assessment_policy(f, edit)))
        case ValidForm(_, data) => {
          for {
            businessActivity <-
            dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivity.riskAssessmentspolicy(data)
            )
          } yield edit match {
            case true => Redirect(routes.WhatYouNeedController.get())
            case false => Redirect(routes.AccountantForAMLSRegulationsController.get())
          }
        }
      }
  }
}

object RiskAssessmentController extends RiskAssessmentController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
