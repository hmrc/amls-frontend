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
          case Some(data) => Ok(person_registered(EmptyForm, data.count(_.personName.isDefined)))
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
                case true => Future.successful(Redirect(routes.ResponsiblePeopleAddController.get(false)))
                case false => Future.successful(Redirect(routes.CheckYourAnswersController.get()))
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
