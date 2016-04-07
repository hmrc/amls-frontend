package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.responsiblepeople.{PersonAddressHistory, ResponsiblePeople}
import utils.RepeatingSection
import views.html.responsiblepeople._

import scala.concurrent.Future

trait PersonAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            /*case Some(ResponsiblePeople(_, _, Some(data))) =>
              Ok(person_address(Form2[PersonAddressHistory](data), edit, index))*/
            case _ =>
              Ok(person_address(EmptyForm, edit, index))
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          Form2[PersonAddressHistory](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(person_address(f, edit, index)))
            /*case ValidForm(_, data) =>
              for {
                _ <- updateData[ResponsiblePeople](index) {
                  case _ => Some(ResponsiblePeople(personAddressHistory = Some(data)))
                }
              } yield {
                Redirect(routes.AddPersonController.get(index, edit))
              }*/
          }
      }
    }
}

object PersonAddressController extends PersonAddressController{
  override val authConnector = AMLSAuthConnector
  override def dataCacheConnector = DataCacheConnector
}

