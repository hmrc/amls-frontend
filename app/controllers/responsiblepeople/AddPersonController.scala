package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{AddPerson, ResponsiblePeople}
import utils.RepeatingSection

import scala.concurrent.Future

trait AddPersonController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(Some(data), _, _)) =>
              Ok(views.html.responsiblepeople.add_person(Form2[AddPerson](data), edit, index))
            case _ =>
              Ok(views.html.responsiblepeople.add_person(EmptyForm, edit, index))
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request => {
          Form2[AddPerson](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(views.html.responsiblepeople.add_person(f, edit, index)))
            case ValidForm(_, data) =>
              for {
                _ <- updateData[ResponsiblePeople](index) {
                  case _ => Some(ResponsiblePeople(Some(data)))
                }
              } yield {
                Redirect(routes.AddPersonController.get(index, edit))
              }
          }
        }
      }
    }

}

object AddPersonController extends AddPersonController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
