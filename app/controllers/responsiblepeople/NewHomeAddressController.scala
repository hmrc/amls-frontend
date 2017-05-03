package controllers.responsiblepeople

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ThreeYearsPlus, ZeroToFiveMonths}
import models.responsiblepeople._
import org.joda.time.{LocalDate, Months}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.new_home_address

import scala.concurrent.Future

@Singleton
class NewHomeAddressController @Inject()(val authConnector: AuthConnector,
                                         val dataCacheConnector: DataCacheConnector) extends RepeatingSection with BaseController {

  final val DefaultAddressHistory = NewHomeAddress(PersonAddressUK("", "", None, None, ""))

  def get(index: Int) =
    Authorised.async {
      implicit authContext =>
        implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _))
            => Ok(new_home_address(Form2(DefaultAddressHistory), index, personName.titleName))
            case _
            => NotFound(notFoundView)
          }
    }

  private def getTimeAtAddress(dateOfMove:  Option[NewHomeDateOfChange]): Option[TimeAtAddress] = {
    dateOfMove map {
      date =>
        Months.monthsBetween(LocalDate.now(), date.dateOfChange).getMonths match {
          case m if 0 until 5 contains m  => ZeroToFiveMonths
          case m if 6 until 11 contains m => SixToElevenMonths
          case m if 12 until 36 contains m => OneToThreeYears
          case _ => ThreeYearsPlus
        }
    }
  }

  private def convertToCurrentAddress(addr: NewHomeAddress, dateOfMove: Option[NewHomeDateOfChange], rp: ResponsiblePeople) = {
    println("================================================================="+addr)
    ResponsiblePersonAddressHistory(Some(ResponsiblePersonCurrentAddress(addr.personAddress,
      getTimeAtAddress(dateOfMove),
      dateOfMove.fold[Option[DateOfChange]](None)(x => Some(DateOfChange(x.dateOfChange))))),
      rp.addressHistory.fold[Option[ResponsiblePersonAddress] ](None)(_.additionalAddress),
      rp.addressHistory.fold[Option[ResponsiblePersonAddress] ](None)(_.additionalExtraAddress)
    )
  }

  def post(index: Int) =
    Authorised.async {
      implicit authContext =>
        implicit request =>
          (Form2[NewHomeAddress](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePeople](index) map { rp =>
                BadRequest(new_home_address(f, index, ControllerHelper.rpTitleName(rp)))
              }
            case ValidForm(_, data) => {
              for {
                moveDate <- dataCacheConnector.fetch[NewHomeDateOfChange](NewHomeDateOfChange.key)
                result <- updateDataStrict[ResponsiblePeople](index) { rp => rp
                  //rp.addressHistory(convertToCurrentAddress(data, moveDate, rp))
                }
              } yield Redirect(routes.DetailedAnswersController.get(index))
            }
          }).recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
    }
}
