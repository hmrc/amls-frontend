package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.responsiblepeople.{PersonName, ResponsiblePeople}
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
            response =>
              val form = (for {
                responsiblePeople <- response
                person <- responsiblePeople.personName
              } yield Form2[PersonName](person)).getOrElse(EmptyForm)
              Ok(person_name(form, edit, index))
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
            case ValidForm(_, data) =>
              for {
                _ <- updateData[ResponsiblePeople](index) {
                  case Some(rp) => Some(rp.personName(data))
                  case _ => Some(ResponsiblePeople(Some(data)))
                }
              } yield {
                Redirect(routes.PersonResidentTypeController.get(index, edit))
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
