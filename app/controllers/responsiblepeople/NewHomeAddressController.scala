package controllers.responsiblepeople

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import org.joda.time.LocalDate
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, DateOfChangeHelper, RepeatingSection}
import views.html.responsiblepeople.new_home_address

import scala.concurrent.Future

@Singleton
class NewHomeAddressController @Inject()(val authConnector: AuthConnector,
                                         val dataCacheConnector: DataCacheConnector) extends RepeatingSection with BaseController with DateOfChangeHelper {

  final val DefaultAddressHistory = NewHomeAddress(PersonAddressUK("", "", None, None, ""))

  def get(index: Int, edit: Boolean = false) =
    Authorised.async {
      implicit authContext =>
        implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _))
            => Ok(new_home_address(Form2(DefaultAddressHistory), edit, index, personName.titleName))
            case _
            => NotFound(notFoundView)
          }
    }

 /* def getTimeAtAddress(dateOfMove: ResponsiblePersonEndDate): Option[TimeAtAddress] = {
    dateOfMove.endDate.compareTo(LocalDate.now)
  }

  def convertToCurrentAddress(addr: NewHomeAddress, dateOfMove: Option[ResponsiblePersonEndDate]) = {
    ResponsiblePersonAddressHistory(Some(ResponsiblePersonCurrentAddress(addr.personAddress,
      getTimeAtAddress(dateOfMove),
      dateOfMove)))
  }*/

  def post(index: Int, edit: Boolean = false) =
    Authorised.async {
      implicit authContext =>
        implicit request =>
          (Form2[NewHomeAddress](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePeople](index) map { rp =>
                BadRequest(new_home_address(f, edit, index, ControllerHelper.rpTitleName(rp)))
              }
            case ValidForm(_, data) => {
              for {
                _ <- updateDataStrict[ResponsiblePeople](index) { rp => rp
                  //rp.addressHistory(convertToCurrentAddress(data, rp.endDate))
                }
              } yield Redirect(routes.CurrentAddressDateOfChangeController.get(index))
            }
          }).recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
    }
}
