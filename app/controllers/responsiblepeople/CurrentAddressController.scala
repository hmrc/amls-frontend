package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress.{Empty, ZeroToFiveMonths, ThreeYearsPlus}
import models.responsiblepeople.{PersonAddressUK, ResponsiblePersonAddressHistory, ResponsiblePersonAddress, ResponsiblePeople}
import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.RepeatingSection
import views.html.responsiblepeople._

import scala.concurrent.Future

trait CurrentAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), Empty)

  def get(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            response =>
              val form = (for {
                res <- response
                addressHistory <- res.addressHistory
                currentAddress <- addressHistory.currentAddress
              } yield Form2[ResponsiblePersonAddress](currentAddress)).getOrElse(Form2(DefaultAddressHistory))
              Ok(current_address(form, edit, index))
          }
      }
    }


  def post(index: Int, edit: Boolean = false) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          Form2[ResponsiblePersonAddress](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(current_address(f, edit, index)))
            case ValidForm(_, data) =>
              for {
                _ <- doUpdate(index, data)
              } yield {
                Logger.debug(s"${data.timeAtAddress} -- $edit")
                (data.timeAtAddress, edit) match {
                case (ThreeYearsPlus, false) => Redirect(routes.PositionWithinBusinessController.get(index, edit))
                case (_, false) => Redirect(routes.AdditionalAddressController.get(index, edit))
                case (ThreeYearsPlus, true) => Redirect(routes.DetailedAnswersController.get(index))
                case (_, true) => Redirect(routes.AdditionalAddressController.get(index, edit))
              }}
          }
      }
    }

  private def doUpdate(index: Int, data: ResponsiblePersonAddress)(implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateData[ResponsiblePeople](index) {
      case Some(res) => {
        Some(res.addressHistory(
          (res.addressHistory, data.timeAtAddress) match {
            case (Some(a), ThreeYearsPlus) => ResponsiblePersonAddressHistory(currentAddress = Some(data))
            case (Some(a), _) => a.currentAddress(data)
            case _ => ResponsiblePersonAddressHistory(currentAddress = Some(data))
          })
        )
      }
      case _ =>
        Some(ResponsiblePeople(
          addressHistory = Some(ResponsiblePersonAddressHistory(
            currentAddress = Some(data)))))
    }
  }

}

object CurrentAddressController extends CurrentAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override def dataCacheConnector = DataCacheConnector
}
