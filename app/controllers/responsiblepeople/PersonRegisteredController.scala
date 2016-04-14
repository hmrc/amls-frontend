package controllers.responsiblepeople

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.{PersonRegistered, VATRegistered, ResponsiblePeople}
import utils.RepeatingSection
import views.html.responsiblepeople._

import scala.concurrent.Future

trait PersonRegisteredController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int) =
  ResponsiblePeopleToggle {
    Authorised.async {
      implicit authContext => implicit request =>
        Future.successful(Ok(person_registered(EmptyForm, index)))
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
               data.registerAnother match {
                case false => Future.successful(Redirect(routes.AddPersonController.get(index + 1, false)))
                case true  => Future.successful(Redirect(routes.SummaryController.get()))
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
