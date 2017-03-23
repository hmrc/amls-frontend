package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress._
import models.responsiblepeople._
import models.status.{SubmissionDecisionApproved, SubmissionStatus}
import play.api.mvc.{AnyContent, Request}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, DateOfChangeHelper, RepeatingSection}
import views.html.responsiblepeople.current_address

import scala.concurrent.Future

trait CurrentAddressController extends RepeatingSection with BaseController with DateOfChangeHelper {

  def dataCacheConnector: DataCacheConnector

  val statusService: StatusService


  final val DefaultAddressHistory = ResponsiblePersonCurrentAddress(PersonAddressUK("", "", None, None, ""), Empty)

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>

        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName), _, _,
          Some(ResponsiblePersonAddressHistory(Some(currentAddress), _, _)), _,_, _, _, _, _, _, _, _, _, _))
          => Ok(current_address(Form2[ResponsiblePersonCurrentAddress](currentAddress), edit, index, fromDeclaration, personName.titleName))
          case Some(ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _,_))
          => Ok(current_address(Form2(DefaultAddressHistory), edit, index, fromDeclaration, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
    }

  def handleRedirection(index: Int, data: ResponsiblePersonCurrentAddress,
                        rpO: Option[ResponsiblePeople],
                        status: SubmissionStatus,
                        edit: Boolean,
                        fromDeclaration: Boolean)(implicit request:Request[AnyContent]) = {
    status match {
      case SubmissionDecisionApproved => {
        rpO match {
          case None => NotFound(notFoundView)
          case Some(rp) => {
            rp.addressHistory match {
              case None =>
                handleApproved(index, edit, None, rp.lineId, data, fromDeclaration)
              case Some(hist) => {
                hist.currentAddress match {
                  case None =>
                    handleApproved(index, edit, None, rp.lineId, data, fromDeclaration)
                  case Some(currAdd) =>
                    handleApproved(index, edit, Some(currAdd.personAddress), rp.lineId, data, fromDeclaration)
                }
              }
            }
          }
        }
      }
      case _ => handleNotYetApproved(index, data.timeAtAddress, edit, fromDeclaration)
    }
  }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        (Form2[ResponsiblePersonCurrentAddress](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map { rp =>
              BadRequest(current_address(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            val responsiblePersonF = getData[ResponsiblePeople](index)
            doUpdate(index, data).flatMap { _ =>
              for {
                rpO <- responsiblePersonF
                status <- statusService.getStatus
              } yield handleRedirection(index, data, rpO, status, edit, fromDeclaration)
            }
          }
        }).recoverWith {
          case _: IndexOutOfBoundsException =>
            Future.successful(NotFound(notFoundView))
        }
    }

  private def handleApproved(index: Int,
                              edit: Boolean,
                              originalPersonAddress: Option[PersonAddress],
                              lineId: Option[Int],
                              data: ResponsiblePersonCurrentAddress, fromDeclaration: Boolean = false) = {

    val moreThanOneYear = (data.timeAtAddress == ThreeYearsPlus) || data.timeAtAddress == OneToThreeYears


    if (redirectToDateOfChange[PersonAddress](originalPersonAddress, data.personAddress)
      && lineId.isDefined && originalPersonAddress.isDefined) {
      Redirect(routes.CurrentAddressDateOfChangeController.get(index, edit))
    } else if (moreThanOneYear && !edit) {
      Redirect(routes.PositionWithinBusinessController.get(index, edit, fromDeclaration))
    } else if (!moreThanOneYear) {
      Redirect(routes.AdditionalAddressController.get(index, edit, fromDeclaration))
    } else {
      Redirect(routes.DetailedAnswersController.get(index, edit))
    }
  }

  private def handleNotYetApproved(index: Int,
                                   timeAtAddress: TimeAtAddress,
                                   edit: Boolean, fromDeclaration: Boolean = false) = {
    (timeAtAddress, edit) match {
      case (ThreeYearsPlus | OneToThreeYears, false) => Redirect(routes.PositionWithinBusinessController.get(index, edit, fromDeclaration))
      case (_, false) => Redirect(routes.AdditionalAddressController.get(index, edit, fromDeclaration))
      case (ThreeYearsPlus | OneToThreeYears, true) => Redirect(routes.DetailedAnswersController.get(index, edit))
      case (_, true) => Redirect(routes.AdditionalAddressController.get(index, edit, fromDeclaration))
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
