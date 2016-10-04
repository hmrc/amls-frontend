package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.responsiblepeople.{ContactDetails, ResponsiblePeople}
import utils.RepeatingSection
import views.html.responsiblepeople.contact_details

import scala.concurrent.Future

trait ContactDetailsController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(_, _, Some(name), _, _, _, _, _, _, _, _, _, _,_))
              => Ok(contact_details(Form2[ContactDetails](name), edit, index))
            case Some(ResponsiblePeople(_, _, _, _, _, _, _, _, _, _, _, _, _,_))
              => Ok(contact_details(EmptyForm, edit, index))
            case _
              => NotFound(notFoundView)
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request => {

          Form2[ContactDetails](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(views.html.responsiblepeople.contact_details(f, edit, index)))
            case ValidForm(_, data) => {
              for {
                result <- updateDataStrict[ResponsiblePeople](index) { rp =>
                  rp.contactDetails(data)
                }
              } yield edit match {
                case true => Redirect(routes.DetailedAnswersController.get(index))
                case false => Redirect(routes.CurrentAddressController.get(index, edit))
              }
            }.recoverWith {
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
            }
          }
        }

      }
    }
}

object ContactDetailsController extends ContactDetailsController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
