package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{PersonAddressUK, ResponsiblePeople, ResponsiblePersonAddress, ResponsiblePersonAddressHistory}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.additional_extra_address

import scala.concurrent.Future

trait AdditionalExtraAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[ResponsiblePeople](index) map {
        case Some(ResponsiblePeople(Some(personName),_,_,Some(ResponsiblePersonAddressHistory(_,_,Some(additionalExtraAddress))),_,_,_,_,_,_,_,_,_, _)) =>
          Ok(additional_extra_address(Form2[ResponsiblePersonAddress](additionalExtraAddress), edit, index, fromDeclaration, personName.titleName))
        case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
          Ok(additional_extra_address(Form2(DefaultAddressHistory), edit, index, fromDeclaration, personName.titleName))
        case _ => NotFound(notFoundView)
      }
  }
  
  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      (Form2[ResponsiblePersonAddress](request.body) match {
        case f: InvalidForm =>
          getData[ResponsiblePeople](index) map { rp =>
            BadRequest(additional_extra_address(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
          }
        case ValidForm(_, data) => {
          getData[ResponsiblePeople](index) flatMap { responsiblePerson =>
            (for {
              rp <- responsiblePerson
              addressHistory <- rp.addressHistory
              additionalExtraAddress <- addressHistory.additionalExtraAddress
            } yield {
              val additionalExtraAddressWithTime = data.copy(timeAtAddress = additionalExtraAddress.timeAtAddress)
              updateAndRedirect(additionalExtraAddressWithTime, index, edit, fromDeclaration)
            }) getOrElse updateAndRedirect(data, index, edit, fromDeclaration)
          }
        }
      }).recoverWith {
        case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
      }
    }
  }

  private def updateAndRedirect
  (data: ResponsiblePersonAddress, index: Int, edit: Boolean, fromDeclaration: Boolean)
  (implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePeople](index) { res =>
      res.addressHistory(
        res.addressHistory match {
          case Some(a) => a.additionalExtraAddress(data)
          case _ => ResponsiblePersonAddressHistory(additionalExtraAddress = Some(data))
        }
      )
    } map { _ =>
      edit match {
        case true => Redirect(routes.DetailedAnswersController.get(index))
        case false => Redirect(routes.TimeAtAdditionalExtraAddressController.get(index, edit, fromDeclaration))
      }
    }
  }
}

object AdditionalExtraAddressController extends AdditionalExtraAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
