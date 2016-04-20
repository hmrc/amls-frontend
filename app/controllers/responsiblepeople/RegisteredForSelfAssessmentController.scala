package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
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
            response =>
              val form = (for {
                resp <- response
                person <- resp.saRegistered
              } yield Form2[SaRegistered](person)).getOrElse(EmptyForm)
              Ok(registered_for_self_assessment(form, edit, index))
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
                  case Some(rp) => Some(rp.saRegistered(data))
                  case _ => Some(ResponsiblePeople(saRegistered = Some(data)))
                }
              } yield {
                Redirect(routes.TrainingController.get(index, edit))
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

