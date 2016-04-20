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
            response =>
              val form = (for {
                responsiblePeople <- response
                contactDetails <- responsiblePeople.contactDetails
              } yield Form2[ContactDetails](contactDetails)).getOrElse(EmptyForm)
              Ok(contact_details(form, edit, index))
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
            case ValidForm(_, data) =>
              for {
                _ <- updateData[ResponsiblePeople](index) {
                  case Some(rp) => Some(rp.contactDetails(data))
                  case _ => Some(ResponsiblePeople(contactDetails = Some(data)))
                }
              } yield edit match {
                case false => Redirect(routes.CurrentAddressController.get(index, edit))
                case true => Redirect(routes.SummaryController.get())
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
