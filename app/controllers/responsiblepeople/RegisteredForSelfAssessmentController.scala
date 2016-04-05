package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.responsiblepeople.{SaRegistered, ResponsiblePeople}
import views.html.responsiblepeople._

import scala.concurrent.Future

trait RegisteredForSelfAssessmentController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[ResponsiblePeople](ResponsiblePeople.key) map {
        case Some(ResponsiblePeople(Some(data))) =>
          Ok(registered_for_self_assessment(Form2[SaRegistered](data), edit))
        case _ =>
          Ok(registered_for_self_assessment(EmptyForm, edit))
      }
      }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[SaRegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(registered_for_self_assessment(f, edit)))
        case ValidForm(_, data) =>
          for {
            resPeople <- dataCacheConnector.fetch[ResponsiblePeople](ResponsiblePeople.key)
            _ <- dataCacheConnector.save[ResponsiblePeople](ResponsiblePeople.key,
              resPeople.saRegistered(data)
            )
          } yield edit match {
            case true =>  Redirect(routes.WhatYouNeedController.get())//Todo
            case false => Redirect(routes.WhatYouNeedController.get())//Todo
          }
      }
  }
}

object RegisteredForSelfAssessmentController extends RegisteredForSelfAssessmentController{
  override val authConnector = AMLSAuthConnector
  override def dataCacheConnector = DataCacheConnector
}

