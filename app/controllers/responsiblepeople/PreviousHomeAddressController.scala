package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.responsiblepeople._
import utils.RepeatingSection
import views.html.responsiblepeople.previous_home_address

import scala.concurrent.Future

trait PreviousHomeAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
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
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request => {
          Form2[PreviousHomeAddress](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(views.html.responsiblepeople.previous_home_address(f, edit, index)))
            case ValidForm(_, data) =>
              for {
                _ <- updateData[ResponsiblePeople](index) {
                  case Some(res) => Some(res.previousHomeAddress(data))
                  case _ => Some(ResponsiblePeople(previousHomeAddress = Some(data)))
                }
              } yield edit match {
                case true => Redirect(routes.SummaryController.get()) //TODO: Responsible Person Details
                case false =>
                  data.TimeAtAddress match {
                    case ThreeYearsPlus => Redirect(routes.PreviousHomeAddressController.get(index, edit)) //TODO: Business Position
                    case _ => Redirect(routes.PreviousHomeAddressController.get(index, edit)) //TODO: Additional Address
                  }
              }
          }
        }
      }
    }
}

object PreviousHomeAddressController extends PreviousHomeAddressController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
