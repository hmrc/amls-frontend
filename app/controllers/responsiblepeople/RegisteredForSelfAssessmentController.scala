package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.responsiblepeople.{SaRegistered, ResponsiblePeople}
import utils.RepeatingSection
import views.html.responsiblepeople._

import scala.concurrent.Future

trait RegisteredForSelfAssessmentController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(_, _, Some(data))) =>
              Ok(registered_for_self_assessment(Form2[SaRegistered](data), edit, index))
            case _ =>
              Ok(registered_for_self_assessment(EmptyForm, edit, index))
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          Form2[SaRegistered](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(registered_for_self_assessment(f, edit, index)))
            case ValidForm(_, data) =>
              for {
                _ <- updateData[ResponsiblePeople](index) {
                  case _ => Some(ResponsiblePeople(saRegistered = Some(data)))
                }
              } yield {
                Redirect(routes.AddPersonController.get(index, edit))
              }
          }
      }
    }
}

object RegisteredForSelfAssessmentController extends RegisteredForSelfAssessmentController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override def dataCacheConnector = DataCacheConnector
}

