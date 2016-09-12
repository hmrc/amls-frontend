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
            case Some(ResponsiblePeople(_, _, _, _, _, Some(person), _, _, _, _, _))
            => Ok(registered_for_self_assessment(Form2[SaRegistered](person), edit, index))
            case Some(ResponsiblePeople(_, _, _, _, _, _, _, _, _, _, _))
            => Ok(registered_for_self_assessment(EmptyForm, edit, index))
            case _
            => NotFound(notFoundView)
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
            case ValidForm(_, data) => {
              for {
                _ <- updateDataStrict[ResponsiblePeople](index) {
                  case Some(rp) => Some(rp.saRegistered(data))

                }
              } yield {
                edit match {
                  case false => Redirect(routes.ExperienceTrainingController.get(index, edit))
                  case true => Redirect(routes.DetailedAnswersController.get(index))
                }
              }
            }.recoverWith {
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
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

