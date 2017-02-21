package controllers.responsiblepeople

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.{PersonRegistered, VATRegistered, ResponsiblePeople}
import utils.{StatusConstants, RepeatingSection}
import views.html.responsiblepeople._

import scala.concurrent.Future

trait PersonRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
          case Some(data) =>
            val count = data.count(x => {
              !x.status.contains(StatusConstants.Deleted) &&
              x.personName.isDefined
            })
            Ok(person_registered(EmptyForm, count, fromDeclaration))
          case _ => Ok(person_registered(EmptyForm, index, fromDeclaration))
        }
  }

  def post(index: Int, fromDeclaration: Boolean = false) =
      Authorised.async {
        implicit authContext => implicit request =>
          Form2[PersonRegistered](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(person_registered(f, index, fromDeclaration)))
            case ValidForm(_, data) =>
               data.registerAnotherPerson match {
                case true => Future.successful(Redirect(routes.ResponsiblePeopleAddController.get(false)))
                case false => Future.successful(Redirect(routes.CheckYourAnswersController.get(fromDeclaration)))
              }
          }
      }
}

object PersonRegisteredController extends PersonRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
