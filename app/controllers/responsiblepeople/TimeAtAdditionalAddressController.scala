package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ThreeYearsPlus}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.time_at_address

import scala.concurrent.Future

trait TimeAtAdditionalAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName),_,_,Some(ResponsiblePersonAddressHistory(_,Some(ResponsiblePersonAddress(_,Some(additionalAddress))),_)),_,_,_,_,_,_,_,_,_,_)) =>
            Ok(time_at_address(Form2[TimeAtAddress](additionalAddress), false, edit, index, fromDeclaration, personName.titleName))
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            Ok(time_at_address(Form2(DefaultAddressHistory), false, edit, index, fromDeclaration, personName.titleName))
          case _ => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
      implicit authContext => implicit request => {
        (Form2[TimeAtAddress](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map {rp =>
              BadRequest(time_at_address(f, false, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) =>
            doUpdate(index, data, DefaultAddressHistory).map { _ =>
              (data, edit) match {
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

  private def doUpdate(index: Int, data: TimeAtAddress, rp: ResponsiblePersonAddress)(implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePeople](index) { res =>
      res.addressHistory(
        (res.addressHistory, rp.timeAtAddress) match {
          case (Some(a), Some(ThreeYearsPlus)) => a.additionalAddress(rp).removeAdditionalExtraAddress
          case (Some(a), Some(OneToThreeYears)) => a.additionalAddress(rp).removeAdditionalExtraAddress
          case (Some(a), _) => a.additionalAddress(rp)
          case _ => ResponsiblePersonAddressHistory(additionalAddress = Some(rp))
        })
    }
  }
}

object TimeAtAdditionalAddressController extends TimeAtAdditionalAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
