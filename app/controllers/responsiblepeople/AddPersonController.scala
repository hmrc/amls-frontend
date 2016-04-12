package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.responsiblepeople.{AddPerson, ResponsiblePeople}
import utils.RepeatingSection
import views.html.responsiblepeople.add_person

import scala.concurrent.Future

trait AddPersonController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            response =>
              val form = (for {
                addperson <- response
                person <- addperson.addPerson
              } yield Form2[AddPerson](person)).getOrElse(EmptyForm)
              Ok(add_person(form, edit, index))
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
                  case Some(rp) => Some(rp.addPerson(data))
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

object AddPersonController extends AddPersonController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
