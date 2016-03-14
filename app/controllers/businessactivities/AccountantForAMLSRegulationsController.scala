package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{AccountantForAMLSRegulations, BusinessActivities}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._

import scala.concurrent.Future

trait AccountantForAMLSRegulationsController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[AccountantForAMLSRegulations] = (for {
            businessActivities <- response
            accountant <- businessActivities.accountantForAMLSRegulations
          } yield Form2[AccountantForAMLSRegulations](accountant)).getOrElse(EmptyForm)
          Ok(accountant_for_amls_regulations(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[AccountantForAMLSRegulations](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(accountant_for_amls_regulations(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.accountantForAMLSRegulations(data)
            )
          } yield (edit, data.accountantForAMLSRegulations) match {
            case (false, true) => Redirect(routes.WhoIsYourAccountantController.get())
            case _ => Redirect(routes.WhatYouNeedController.get())
          }
      }
    }
  }
}

object AccountantForAMLSRegulationsController extends AccountantForAMLSRegulationsController {
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}