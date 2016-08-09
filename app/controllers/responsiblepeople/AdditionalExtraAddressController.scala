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
            case Some(ResponsiblePeople(_, _, _, Some(ResponsiblePersonAddressHistory(_, _, Some(additionalExtraAddress))), _, _, _, _, _, _))
              => Ok(additional_extra_address(Form2[ResponsiblePersonAddress](additionalExtraAddress), edit, index))
            case Some(ResponsiblePeople(_, _, _, _, _, _, _, _, _, _))
              => Ok(additional_extra_address(Form2(DefaultAddressHistory), edit, index))
            case _
              => NotFound(notFoundView)
          }
      }
    }

  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request => {

          (Form2[ResponsiblePersonAddress](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(additional_extra_address(f, edit, index)))
            case ValidForm(_, data) =>
              doUpdate(index, data).map { _ =>
                edit match {
                  case true => Redirect(routes.DetailedAnswersController.get(index))
                  case false => Redirect(routes.PositionWithinBusinessController.get(index, edit))
                }
              }
          }).recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }
    }

  private def doUpdate(index: Int, data: ResponsiblePersonAddress)(implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePeople](index) {
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
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
