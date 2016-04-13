package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2}
import models.responsiblepeople.TimeAtAddress.Empty
import models.responsiblepeople.{ResponsiblePersonAddressHistory, ResponsiblePeople, PersonAddressUK, ResponsiblePersonAddress}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.RepeatingSection
import views.html.responsiblepeople.additional_extra_address

import scala.concurrent.Future

trait AdditionalExtraAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), Empty)

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            response =>
              val form: Form2[ResponsiblePersonAddress] = (for {
                responsiblePeople <- response
                addressHistory <- responsiblePeople.addressHistory
                additionalExtraAddress <- addressHistory.additionalExtraAddress
              } yield Form2[ResponsiblePersonAddress](additionalExtraAddress)).getOrElse(Form2(DefaultAddressHistory))
              Ok(additional_extra_address(form, edit, index))
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request => {
          Form2[ResponsiblePersonAddress](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(views.html.responsiblepeople.additional_extra_address(f, edit, index)))
            case ValidForm(_, data) =>
              for {
                _ <- doUpdate(index, data)
              } yield (data.timeAtAddress, edit) match {
                case (_, false) => Redirect(routes.AdditionalExtraAddressController.get(index, edit)) //TODO: Business Position
                case (_, true) => Redirect(routes.SummaryController.get()) //TODO: Responsible Person Details
              }
          }
        }
      }
    }

  private def doUpdate(index: Int, data: ResponsiblePersonAddress)(implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateData[ResponsiblePeople](index) {
      case Some(res) => {
        Some(res.addressHistory(
          res.addressHistory match {
            case Some(a) => a.additionalExtraAddress(data)
            case _ => ResponsiblePersonAddressHistory(additionalExtraAddress = Some(data))
          })
        )
      }
      case _ =>
        Some(ResponsiblePeople(
          addressHistory = Some(ResponsiblePersonAddressHistory(
            additionalExtraAddress = Some(data)))))
    }
  }

}

object AdditionalExtraAddressController extends AdditionalExtraAddressController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
