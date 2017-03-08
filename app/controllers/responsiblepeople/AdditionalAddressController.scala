package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress.{Empty, OneToThreeYears, ThreeYearsPlus, ZeroToFiveMonths}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.additional_address

import scala.concurrent.Future

trait AdditionalAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), Empty)

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(Some(personName), _, _,
            Some(ResponsiblePersonAddressHistory(_, Some(additionalAddress), _)), _, _, _, _, _, _, _,_,_,_))
              => Ok(additional_address(Form2[ResponsiblePersonAddress](additionalAddress), edit, index, fromDeclaration, personName.titleName))
            case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _,_,_,_))
              => Ok(additional_address(Form2(DefaultAddressHistory), edit, index, fromDeclaration, personName.titleName))
            case _
              => NotFound(notFoundView)
          }
      }


  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
      Authorised.async {
        implicit authContext => implicit request => {
          (Form2[ResponsiblePersonAddress](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePeople](index) map {rp =>
                BadRequest(additional_address(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
              }
            case ValidForm(_, data) =>
              doUpdate(index, data).map { _ =>
                (data.timeAtAddress, edit) match {
                  case (ThreeYearsPlus, false) => Redirect(routes.PositionWithinBusinessController.get(index, edit, fromDeclaration))
                  case (OneToThreeYears, false) => Redirect(routes.PositionWithinBusinessController.get(index, edit, fromDeclaration))
                  case (_, false) => Redirect(routes.AdditionalExtraAddressController.get(index, edit, fromDeclaration))
                  case (ThreeYearsPlus, true) => Redirect(routes.DetailedAnswersController.get(index))
                  case (OneToThreeYears, true) => Redirect(routes.DetailedAnswersController.get(index))
                  case (_, true) => Redirect(routes.AdditionalExtraAddressController.get(index, edit, fromDeclaration))
                }
              }
          }).recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }


  private def doUpdate(index: Int, data: ResponsiblePersonAddress)(implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePeople](index) { res =>
        res.addressHistory(
          (res.addressHistory, data.timeAtAddress) match {
            case (Some(a), ThreeYearsPlus) => a.additionalAddress(data).removeAdditionalExtraAddress
            case (Some(a), OneToThreeYears) => a.additionalAddress(data).removeAdditionalExtraAddress
            case (Some(a), _) => a.additionalAddress(data)
            case _ => ResponsiblePersonAddressHistory(additionalAddress = Some(data))
          })
      }
  }
}

object AdditionalAddressController extends AdditionalAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
