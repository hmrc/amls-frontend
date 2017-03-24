package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.time_at_additional_extra_address

import scala.concurrent.Future

trait TimeAtAdditionalExtraAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[ResponsiblePeople](index) map {
        case Some(ResponsiblePeople(Some(personName),_,_,Some(ResponsiblePersonAddressHistory(_,_,Some(ResponsiblePersonAddress(_, Some(additionalExtraAddress))))),_,_,_,_,_,_,_,_,_,_)) =>
          Ok(time_at_additional_extra_address(Form2[TimeAtAddress](additionalExtraAddress), edit, index, fromDeclaration, personName.titleName))
        case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
          Ok(time_at_additional_extra_address(Form2(DefaultAddressHistory), edit, index, fromDeclaration, personName.titleName))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      (Form2[TimeAtAddress](request.body) match {
        case f: InvalidForm =>
          getData[ResponsiblePeople](index) map { rp =>
            BadRequest(time_at_additional_extra_address(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
          }
        case ValidForm(_, data) =>
          doUpdate(index, data, DefaultAddressHistory).map { _ =>
            edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index))
              case false => Redirect(routes.PositionWithinBusinessController.get(index, edit, fromDeclaration))
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
        res.addressHistory match {
          case Some(a) => a.additionalExtraAddress(rp)
          case _ => ResponsiblePersonAddressHistory(additionalExtraAddress = Some(rp))
        }
      )
    }
  }
}

object TimeAtAdditionalExtraAddressController extends TimeAtAdditionalExtraAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
