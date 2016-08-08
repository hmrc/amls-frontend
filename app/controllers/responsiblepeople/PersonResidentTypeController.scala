package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.responsiblepeople.{PersonResidenceType, ResponsiblePeople}
import utils.RepeatingSection
import views.html.responsiblepeople.person_residence_type

import scala.concurrent.Future

trait PersonResidentTypeController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
    Authorised.async {
      implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(_, Some(residencyType), _, _, _, _, _, _, _, _))
              => Ok(person_residence_type(Form2[PersonResidenceType](residencyType), edit, index))
            case Some(ResponsiblePeople(_, _, _, _, _, _, _, _, _, _))
              => Ok(person_residence_type(EmptyForm, edit, index))
            case _
              => NotFound(notFoundView)
          }
    }
  }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>

          Form2[PersonResidenceType](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(person_residence_type(f, edit, index)))
            case ValidForm(_, data) => {
              for {
                result <- updateDataStrict[ResponsiblePeople](index) { currentData =>
                  Some(currentData.personResidenceType(data))
                }
              } yield edit match {
                case true => Redirect(routes.DetailedAnswersController.get(index))
                case false => Redirect(routes.ContactDetailsController.get(index, edit))
              }
            }.recoverWith {
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
            }
          }
      }
    }
}

object PersonResidentTypeController extends PersonResidentTypeController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override def dataCacheConnector = DataCacheConnector
}

