package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.responsiblepeople.{PreviousHomeAddress, ResponsiblePeople}
import utils.RepeatingSection
import views.html.responsiblepeople.previous_home_address

import scala.concurrent.Future

trait PreviousHomeAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
     getData[ResponsiblePeople](index) map {
        response =>
          val form: Form2[PreviousHomeAddress] = (for {
            responsiblePeople <- response
            previousHomeAddress <- responsiblePeople.previousHomeAddress
          } yield Form2[PreviousHomeAddress](previousHomeAddress)).getOrElse(EmptyForm)
          Ok(previous_home_address(form, edit, index))
      }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[PreviousHomeAddress](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.responsiblepeople.previous_home_address(f, edit, index)))
        case ValidForm(_, data) =>
          for {
            _ <- updateData[ResponsiblePeople](index) {
              case _ => Some(ResponsiblePeople(Some(data)))
            }
          } yield {
            Redirect(routes.PreviousHomeAddressController.get(index, edit))
          }
      }
    }
  }

}

object PreviousHomeAddressController extends PreviousHomeAddressController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
