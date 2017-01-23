package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress._
import models.responsiblepeople._
import models.status.SubmissionDecisionApproved
import play.api.mvc.{AnyContent, Request}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{DateOfChangeHelper, RepeatingSection}
import views.html.responsiblepeople.current_address

import scala.concurrent.Future

trait CurrentAddressController extends RepeatingSection with BaseController with DateOfChangeHelper {

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
          case ValidForm(_, data) => {

            val futureOriginalPersonAddress = getData[ResponsiblePeople](index) map { rpO =>
              for {
                rp <- rpO
                addHist <- rp.addressHistory
                rpCurr <- addHist.currentAddress
              } yield {
                (rpCurr.personAddress, rp.lineId)
              }
            }

            doUpdate(index, data).flatMap { _ =>
              for {
                originalPersonAddress <- futureOriginalPersonAddress
                status <- statusService.getStatus
              } yield {
                status match {
                  case SubmissionDecisionApproved => {
                    originalPersonAddress match {
                      case Some((address, lineID)) => {
                        if (lineID.isEmpty) {
                          handleApprovedWithNoLineId(index, edit, Some(address), data)
                        } else {
                          handleApprovedWithLineId(index, edit, Some(address), data)
                        }
                      }
                      case _ => NotFound(notFoundView)
                    }
                  }
                  case _ => handleNotYetApproved(index, data.timeAtAddress, edit)
                }
              }
            }
          }
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }

  private def handleApprovedWithNoLineId(index: Int,
                                         edit: Boolean,
                                         originalPersonAddress: Option[PersonAddress],
                                         data: ResponsiblePersonCurrentAddress) = {

    val moreThanOneYear = (data.timeAtAddress == ThreeYearsPlus) || data.timeAtAddress == OneToThreeYears

    if (!moreThanOneYear) {
      Redirect(routes.AdditionalAddressController.get(index, edit))
    } else if (moreThanOneYear && !edit) {
      Redirect(routes.PositionWithinBusinessController.get(index, edit))
    } else {
      Redirect(routes.DetailedAnswersController.get(index))
    }

  }

  private def handleApprovedWithLineId(index: Int,
                                       edit: Boolean,
                                       originalPersonAddress: Option[PersonAddress],
                                       data: ResponsiblePersonCurrentAddress) = {

    val moreThanOneYear = (data.timeAtAddress == ThreeYearsPlus) || data.timeAtAddress == OneToThreeYears

    if (redirectToDateOfChange[PersonAddress](originalPersonAddress, data.personAddress)) {
      Redirect(routes.CurrentAddressDateOfChangeController.get(index, edit))
    } else if (moreThanOneYear) {
      Redirect(routes.DetailedAnswersController.get(index))
    } else {
      Redirect(routes.AdditionalAddressController.get(index, edit))
    }
  }

  private def handleNotYetApproved(index: Int,
                                   timeAtAddress: TimeAtAddress,
                                   edit: Boolean) = {
    (timeAtAddress, edit) match {
      case (ThreeYearsPlus | OneToThreeYears, false) => Redirect(routes.PositionWithinBusinessController.get(index, edit))
      case (_, false) => Redirect(routes.AdditionalAddressController.get(index, edit))
      case (ThreeYearsPlus | OneToThreeYears, true) => Redirect(routes.DetailedAnswersController.get(index))
      case (_, true) => Redirect(routes.AdditionalAddressController.get(index, edit))
    }
  }

  private def doUpdate
  (index: Int, data: ResponsiblePersonCurrentAddress)
  (implicit authContext: AuthContext, request: Request[AnyContent]) = {
    updateDataStrict[ResponsiblePeople](index) { res =>
      res.addressHistory(
        (res.addressHistory, data.timeAtAddress) match {
          case (Some(a), ThreeYearsPlus | OneToThreeYears) => ResponsiblePersonAddressHistory(currentAddress = Some(data))
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
