package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import views.html.responsiblepeople._

import scala.concurrent.Future

trait RegisteredForSelfAssessmentController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
          Future.successful(Ok(registered_for_self_assessment(EmptyForm, edit)))
      }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(registered_for_self_assessment(EmptyForm, edit)))
  }
}

object RegisteredForSelfAssessmentController extends RegisteredForSelfAssessmentController{
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}

