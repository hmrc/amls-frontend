package controllers.responsiblepeople

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, DateOfChangeHelper, RepeatingSection}
import views.html.responsiblepeople.current_address

import scala.concurrent.Future

@Singleton
class NewHomeAddressController @Inject()(val authConnector: AuthConnector,
                                         val dataCacheConnector: DataCacheConnector,
                                         val statusService: StatusService) extends RepeatingSection with BaseController with DateOfChangeHelper {


  final val DefaultAddressHistory = ResponsiblePersonCurrentAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext =>
        implicit request =>

          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(Some(personName), _, _,
            Some(ResponsiblePersonAddressHistory(Some(currentAddress), _, _)), _, _, _, _, _, _, _, _, _, _, _))
            => Ok(current_address(Form2[ResponsiblePersonCurrentAddress](currentAddress), edit, index, fromDeclaration, personName.titleName))
            case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _))
            => Ok(current_address(Form2(DefaultAddressHistory), edit, index, fromDeclaration, personName.titleName))
            case _
            => NotFound(notFoundView)
          }
    }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext =>
        implicit request =>
          (Form2[ResponsiblePersonCurrentAddress](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePeople](index) map { rp =>
                BadRequest(current_address(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
              }
            case ValidForm(_, data) => {
              getData[ResponsiblePeople](index) flatMap { responsiblePerson =>
                (for {
                  rp <- responsiblePerson
                  addressHistory <- rp.addressHistory
                  currentAddress <- addressHistory.currentAddress
                } yield data.copy(timeAtAddress = currentAddress.timeAtAddress)).getOrElse(data)
                Future.successful(Redirect(routes.CurrentAddressDateOfChangeController.get(index, edit)))
              }
            }
          }).recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
    }
}
