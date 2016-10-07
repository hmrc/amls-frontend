package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{Nationality, PersonResidenceType, ResponsiblePeople}
import utils.RepeatingSection
import views.html.responsiblepeople.nationality

import scala.concurrent.Future

trait NationalityController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(_, Some(residencyType), _, _, _, _, _, _, _, _, _, _, _))
            => residencyType.nationality match {
                case Some(country) => Ok(nationality(Form2[Nationality](country), edit, index))
                case _ => Ok(nationality(EmptyForm, edit, index))
              }
            case Some(ResponsiblePeople(_, _, _, _, _, _, _, _, _, _, _, _, _))
            => Ok(nationality(EmptyForm, edit, index))
            case _
            => NotFound(notFoundView)
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>

          Form2[Nationality](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(nationality(f, edit, index)))
            case ValidForm(_, data) => {
              for {
                result <- updateDataStrict[ResponsiblePeople](index) { rp =>
                  val residenceType = rp.personResidenceType.map(x => x.copy(nationality = Some(data)))
                  rp.personResidenceType(residenceType)
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

object NationalityController extends NationalityController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector

  override def dataCacheConnector = DataCacheConnector
}

