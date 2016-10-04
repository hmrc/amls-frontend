package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import play.api.i18n.Messages
import play.api.mvc.Request
import utils.RepeatingSection
import views.html.responsiblepeople.person_name

import scala.concurrent.Future

trait PersonNameController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector


  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(Some(name), _, _, _, _, _, _, _, _, _, _, _, _,_))
                => Ok(person_name(Form2[PersonName](name), edit, index))
            case Some(ResponsiblePeople(_, _, _, _, _, _, _, _, _, _, _, _, _,_))
                => Ok(person_name(EmptyForm, edit, index))
            case _
                => NotFound(notFoundView)
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request => {
          Form2[PersonName](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(views.html.responsiblepeople.person_name(f, edit, index)))
            case ValidForm(_, data) => {
              for {
                result <- updateDataStrict[ResponsiblePeople](index) { rp =>
                  rp.personName(data)
                }
              } yield edit match {
                case true => Redirect(routes.DetailedAnswersController.get(index))
                case false => Redirect(routes.PersonResidentTypeController.get(index, edit))
              }
            }.recoverWith {
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
            }
          }
        }
      }
    }

}

object PersonNameController extends PersonNameController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
