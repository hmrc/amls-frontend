package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress._
import models.responsiblepeople._
import models.status.SubmissionDecisionApproved
import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.RepeatingSection
import views.html.responsiblepeople.current_address

import scala.concurrent.Future

trait CurrentAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector
  val statusService: StatusService


  final val DefaultAddressHistory = ResponsiblePersonCurrentAddress(PersonAddressUK("", "", None, None, ""), Empty)

  def get(index: Int, edit: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>

        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(_, _, _, Some(ResponsiblePersonAddressHistory(Some(currentAddress), _, _)), _, _, _, _, _, _, _, _, _, _))
          => Ok(current_address(Form2[ResponsiblePersonCurrentAddress](currentAddress), edit, index))
          case Some(ResponsiblePeople(_, _, _, _, _, _, _, _, _, _, _, _, _, _))
          => Ok(current_address(Form2(DefaultAddressHistory), edit, index))
          case _
          => NotFound(notFoundView)
        }
    }


  def post(index: Int, edit: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        (Form2[ResponsiblePersonCurrentAddress](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(current_address(f, edit, index)))
          case ValidForm(_, data) =>
            doUpdate(index, data).map { _ =>
              val status = statusService.getStatus

              status.map {
                case SubmissionDecisionApproved => ???
                case _ => handleNotYetApproved(index, data.timeAtAddress, edit)
              }

              handleNotYetApproved(index, data.timeAtAddress, edit)
            }
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }

  private def handleNotYetApproved(index: Int, timeAtAddress:TimeAtAddress, edit: Boolean) = {
    (timeAtAddress, edit) match {
      case (ThreeYearsPlus|OneToThreeYears, false) => Redirect(routes.PositionWithinBusinessController.get(index, edit))
      case (_, false) => Redirect(routes.AdditionalAddressController.get(index, edit))
      case (ThreeYearsPlus|OneToThreeYears, true) => Redirect(routes.DetailedAnswersController.get(index))
      case (_, true) => Redirect(routes.AdditionalAddressController.get(index, edit))
    }
  }

  private def handleApproved(index: Int, newTimeAtAddress:TimeAtAddress, oldTimeAtAddress: TimeAtAddress) = {

    def lessThanOneYear(date: TimeAtAddress) = (date == ZeroToFiveMonths) || (date == SixToElevenMonths)
    def moreThanOneYear(date: TimeAtAddress) = (date == OneToThreeYears) || (date == ThreeYearsPlus)

      if (oldTimeAtAddress == newTimeAtAddress) {
        Redirect(routes.CurrentAddressDateOfChangeController.get(index, true))
      } else if (moreThanOneYear(oldTimeAtAddress)) {
        Redirect(routes.CurrentAddressDateOfChangeController.get(index, true)) // need to insert a flag to redirect on from here?? or check this in the next bit instead.
      }
  }

  private def doUpdate
  (index: Int, data: ResponsiblePersonCurrentAddress)
  (implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePeople](index) { res =>
      res.addressHistory(
        (res.addressHistory, data.timeAtAddress) match {
          case (Some(a), ThreeYearsPlus) => ResponsiblePersonAddressHistory(currentAddress = Some(data))
          case (Some(a), OneToThreeYears) => ResponsiblePersonAddressHistory(currentAddress = Some(data))
          case (Some(a), _) => a.currentAddress(data)
          case _ => ResponsiblePersonAddressHistory(currentAddress = Some(data))
        })
    }
  }
}

object CurrentAddressController extends CurrentAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  val statusService = StatusService

  override def dataCacheConnector = DataCacheConnector
}
