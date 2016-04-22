package controllers.responsiblepeople

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.{PersonRegistered, VATRegistered, ResponsiblePeople}
import utils.RepeatingSection
import views.html.responsiblepeople._

import scala.concurrent.Future

trait PersonRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int) =
  ResponsiblePeopleToggle {
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
          case Some(data) => Ok(person_registered(EmptyForm, data.size))
          case _ => Ok(person_registered(EmptyForm, index))
        }
    }
  }

  def post(index: Int) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          Form2[PersonRegistered](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(person_registered(f, index)))
            case ValidForm(_, data) =>
               data.registerAnotherPerson match {
                case true => Future.successful(Redirect(routes.PersonNameController.get(index + 1, false)))
                case false => Future.successful(Redirect(routes.YourAnswersController.get(false)))
              }
          }
      }
    }
}

object PersonRegisteredController extends PersonRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
