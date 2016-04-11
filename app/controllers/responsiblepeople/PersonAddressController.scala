package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.AddressHistory._
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
            response =>
              val form = (for {
                res <- response
                addressHistory <- res.personAddressHistory
              } yield Form2[PersonAddressHistory](addressHistory)).getOrElse(EmptyForm)
              Ok(person_address(form, edit, index))
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
            case ValidForm(_, data) =>
              for {
                _ <- updateData[ResponsiblePeople](index) {
                  case Some(rp) => Some(rp.personAddressHistory(data))
                  case _ => Some(ResponsiblePeople(personAddressHistory = Some(data)))
                }
              } yield data.addressHistory match {
                case First | Second | Third => Redirect(routes.SummaryController.get()) //TODO Redirect to page
                case Fourth => Redirect(routes.AddPersonController.get(index, edit))
              }
          }
      }
    }
}

object PersonAddressController extends PersonAddressController {
  override val authConnector = AMLSAuthConnector

  override def dataCacheConnector = DataCacheConnector
}
